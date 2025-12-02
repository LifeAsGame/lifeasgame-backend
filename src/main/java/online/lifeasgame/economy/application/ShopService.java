package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.ReservationToken;
import online.lifeasgame.economy.domain.ShopItem;
import online.lifeasgame.economy.domain.ShopPurchase;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.domain.event.EconomyEventType;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopItemReader shopItemReader;
    private final ShopItemWriter shopItemWriter;
    private final ShopPurchaseReader shopPurchaseReader;
    private final ShopPurchaseWriter shopPurchaseWriter;
    private final WalletWriter walletWriter;
    private final WalletHoldReader walletHoldReader;
    private final IdempotencyKeyStore idempotencyKeyStore;
    private final ShopReservationLimiter shopReservationLimiter;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public EconomyResult.ShopItemView createItem(EconomyCommand.CreateShopItem command) {
        Money price = Money.of(command.price(), command.currency());
        ShopItem saved = shopItemWriter.create(
                command.itemId(),
                price,
                command.globalLimit(),
                command.perPlayerLimit(),
                command.reservationTtlSeconds()
        );
        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.SHOP_ITEM_CREATED)
                        .shopItemId(saved.getId())
                        .actorId(null)
                        .occurredAt(Instant.now())
                        .build()
        );
        return EconomyResult.ShopItemView.from(saved);
    }

    @Transactional
    public EconomyResult.ShopPurchaseId purchase(Long playerId, EconomyCommand.PurchaseShopItem command) {
        if (!idempotencyKeyStore.acquire(command.idempotencyKey(), Duration.ofMinutes(5))) {
            throw new DomainException(EconomyError.DUPLICATE_REQUEST);
        }
        ShopItem item = shopItemReader.getForUpdate(command.shopItemId());
        if (!item.isAvailable()) {
            throw new DomainException(EconomyError.SHOP_ITEM_DISABLED);
        }

        Instant now = Instant.now();
        long completedCount = shopPurchaseReader.countCompleted(item.getId());
        long reservedCount = shopPurchaseReader.countReserved(item.getId(), now);
        if (item.getGlobalStockLimit() != null && completedCount + reservedCount >= item.getGlobalStockLimit()) {
            throw new DomainException(EconomyError.SHOP_STOCK_EXHAUSTED);
        }
        long completedByPlayer = shopPurchaseReader.countCompletedByPlayer(item.getId(), playerId);
        if (item.getPerPlayerLimit() != null && completedByPlayer >= item.getPerPlayerLimit()) {
            throw new DomainException(EconomyError.SHOP_PER_PLAYER_LIMIT);
        }

        boolean redisGuarded = shopReservationLimiter.tryReserve(
                item.getId(),
                playerId,
                command.quantity(),
                item.getGlobalStockLimit(),
                item.getPerPlayerLimit(),
                Duration.ofSeconds(Math.max(item.getReservationTtlSec(), 30))
        );
        if (!redisGuarded) {
            throw new DomainException(EconomyError.SHOP_STOCK_EXHAUSTED);
        }

        try {
            var purchase = shopPurchaseWriter.request(item.getId(), playerId, command.quantity(), item.getPrice());
            var wallet = walletWriter.getOrCreateForUpdate(playerId);
            int ttl = item.getReservationTtlSec();
            Money total = item.getPrice().multiply(command.quantity());
            if (command.reserveOnly() || ttl > 0) {
                ttl = Math.max(ttl, 30);
                String holdId = wallet.placeHold(total, "shop-reserve" + item.getId(), now, ttl);
                ReservationToken token = ReservationToken.newToken();
                purchase.reserve(token, now.plusSeconds(ttl), holdId);
                walletWriter.save(wallet);
                ShopPurchase saved = shopPurchaseWriter.save(purchase);
                domainEventPublisher.publish(
                        EconomyEvent.builder(EconomyEventType.SHOP_PURCHASE_RESERVED)
                                .actorId(playerId)
                                .shopItemId(item.getId())
                                .shopPurchaseId(saved.getId())
                                .reservationToken(token.value())
                                .occurredAt(now)
                                .build()
                );
                return EconomyResult.ShopPurchaseId.of(saved.getId());
            } else {
                wallet.withdraw(total);
                purchase.completeImmediately();
                walletWriter.save(wallet);
                ShopPurchase saved = shopPurchaseWriter.save(purchase);
                domainEventPublisher.publish(
                        EconomyEvent.builder(EconomyEventType.SHOP_PURCHASE_COMPLETED)
                                .actorId(playerId)
                                .shopItemId(item.getId())
                                .shopPurchaseId(saved.getId())
                                .occurredAt(now)
                                .build()
                );
                return EconomyResult.ShopPurchaseId.of(saved.getId());
            }
        } catch (RuntimeException ex) {
            shopReservationLimiter.release(item.getId(), playerId, command.quantity());
            throw ex;
        }
    }

    @Transactional
    public EconomyResult.ShopReservation confirmReservation(Long playerId, EconomyCommand.ConfirmShopReservation command) {
        ShopPurchase purchase = shopPurchaseReader.getByReservationToken(command.reservationToken());
        if (!playerId.equals(purchase.getPlayerId())) {
            throw new DomainException(EconomyError.LISTING_RESERVED_OTHER);
        }
        Instant now = Instant.now();
        if (purchase.getReservationExpiresAt() != null && now.isAfter(purchase.getReservationExpiresAt())) {
            purchase.expire(now);
            shopPurchaseWriter.save(purchase);
            throw new DomainException(EconomyError.LISTING_RESERVATION_EXPIRED);
        }
        var wallet = walletWriter.getOrCreateForUpdate(playerId);
        wallet.commitHold(purchase.getWalletHoldId());
        purchase.completeFromReservation(command.reservationToken());
        walletWriter.save(wallet);
        shopPurchaseWriter.save(purchase);
        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.SHOP_PURCHASE_COMPLETED)
                        .actorId(playerId)
                        .shopItemId(purchase.getShopItemId())
                        .shopPurchaseId(purchase.getId())
                        .reservationToken(command.reservationToken())
                        .occurredAt(now)
                        .build()
        );
        return EconomyResult.ShopReservation.from(purchase);
    }

    @Transactional
    public void expireReservations() {
        Instant now = Instant.now();
        List<ShopPurchase> purchases = shopPurchaseReader.findExpiringBefore(now);
        for (ShopPurchase purchase : purchases) {
            String holdId = purchase.getWalletHoldId();
            purchase.expire(now);
            if (holdId != null) {
                walletHoldReader.findByHoldId(holdId)
                        .ifPresent(hold -> {
                            hold.getWallet().expireHolds(now);
                            walletWriter.save(hold.getWallet());
                        });
            }
            shopPurchaseWriter.save(purchase);
            shopReservationLimiter.release(purchase.getShopItemId(), purchase.getPlayerId(), purchase.getQuantity());
            domainEventPublisher.publish(
                    EconomyEvent.builder(EconomyEventType.SHOP_RESERVATION_EXPIRED)
                            .actorId(purchase.getPlayerId())
                            .shopItemId(purchase.getShopItemId())
                            .shopPurchaseId(purchase.getId())
                            .reservationToken(purchase.getReservationToken())
                            .occurredAt(now)
                            .build()
            );
        }
    }

    @Transactional
    public void toggleAvailability(EconomyCommand.ToggleShopItem command) {
        ShopItem item = shopItemReader.getForUpdate(command.shopItemId());
        if (command.enabled()) {
            item.enable();
        } else {
            item.disable();
        }
        shopItemWriter.save(item);
        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.SHOP_ITEM_TOGGLED)
                        .shopItemId(item.getId())
                        .actorId(null)
                        .attribute("available", item.isAvailable())
                        .occurredAt(Instant.now())
                        .build()
        );
    }

    @Transactional
    public EconomyResult.ShopItemView updateLimits(EconomyCommand.UpdateShopItem command) {
        ShopItem item = shopItemReader.getForUpdate(command.shopItemId());
        item.changeLimits(command.globalLimit(), command.perPlayerLimit(), command.reservationTtlSeconds());
        ShopItem saved = shopItemWriter.save(item);
        return EconomyResult.ShopItemView.from(saved);
    }

    @Transactional(readOnly = true)
    public EconomyResult.ShopItems listAvailableItems() {
        var items = shopItemReader.listAvailable().stream()
                .map(EconomyResult.ShopItemView::from)
                .toList();
        return new EconomyResult.ShopItems(items);
    }

    @Transactional(readOnly = true)
    public EconomyResult.ShopItems listAllItems() {
        var items = shopItemReader.listAll().stream()
                .map(EconomyResult.ShopItemView::from)
                .toList();
        return new EconomyResult.ShopItems(items);
    }

    @Transactional(readOnly = true)
    public EconomyResult.ShopPurchases listPurchases(Long playerId) {
        var purchases = shopPurchaseReader.findByPlayer(playerId).stream()
                .map(EconomyResult.ShopPurchaseView::from)
                .toList();
        return new EconomyResult.ShopPurchases(purchases);
    }

    @Transactional(readOnly = true)
    public EconomyResult.ShopPurchases listAllPurchases() {
        var purchases = shopPurchaseReader.findAll().stream()
                .map(EconomyResult.ShopPurchaseView::from)
                .toList();
        return new EconomyResult.ShopPurchases(purchases);
    }
}
