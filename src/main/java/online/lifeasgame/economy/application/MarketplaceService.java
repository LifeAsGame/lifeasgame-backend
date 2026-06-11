package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.*;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.domain.event.EconomyEventType;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final ListingReader listingReader;
    private final ListingWriter listingWriter;
    private final TradeWriter tradeWriter;
    private final TradeReader tradeReader;
    private final WalletWriter walletWriter;
    private final WalletReader walletReader;
    private final WalletHoldReader walletHoldReader;
    private final IdempotencyKeyStore idempotencyKeyStore;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public EconomyResult.ListingId open(Long sellerId, EconomyCommand.OpenListing command) {
        Currency currency = Currency.parseOptional(command.currency(), Currency.GOLD);
        Money price = Money.of(command.price(), currency);

        Listing listing = listingWriter.create(
                Listing.open(
                        sellerId,
                        command.itemInstanceId(),
                        command.itemId(),
                        price
                )
        );

        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.LISTING_OPENED)
                        .listingId(listing.getId())
                        .actorId(sellerId)
                        .occurredAt(Instant.now())
                        .build()
        );

        return new EconomyResult.ListingId(listing.getId());
    }

    @Transactional
    public EconomyResult.Reservation reserve(Long buyerId, EconomyCommand.ReserveListing command) {
        Listing listing = listingReader.getForUpdate(command.listingId());
        Instant now = Instant.now();
        listing.expire(now);

        if (listing.getStatus() != ListingStatus.OPEN) {
            throw new DomainException(EconomyError.LISTING_NOT_AVAILABLE);
        }

        Wallet wallet = lockOrCreateWallet(buyerId);
        String holdId = wallet.placeHold(
                listing.getPrice(),
                "listing-reserve" + listing.getId(),
                now,
                command.ttlSeconds()
        );

        ReservationToken token = listing.reserve(
                buyerId,
                holdId,
                now,
                command.ttlSeconds()
        );

        listingWriter.create(listing);

        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.LISTING_RESERVED)
                        .listingId(listing.getId())
                        .actorId(buyerId)
                        .reservationToken(token.value())
                        .occurredAt(now)
                        .build()
        );

        return new EconomyResult.Reservation(token.value(), holdId, listing.getReservationExpiresAt());
    }

    @Transactional
    public EconomyResult.TradeSummary purchase(Long buyerId, EconomyCommand.PurchaseListing command) {
        if (!idempotencyKeyStore.acquire(command.idempotencyKey(), Duration.ofMinutes(5))) {
            throw new DomainException(EconomyError.DUPLICATE_REQUEST);
        }

        Listing listing = listingReader.getForUpdate(command.listingId());

        Instant now = Instant.now();
        listing.expire(now);
        if (buyerId.equals(listing.getSellerPlayerId())) {
            throw new DomainException(EconomyError.CANNOT_PURCHASE_OWN_LISTING);
        }

        Wallet buyerWallet = lockOrCreateWallet(buyerId);
        Wallet sellerWallet = lockOrCreateWallet(listing.getSellerPlayerId());

        if (listing.getStatus() == ListingStatus.RESERVED) {
            if (listing.getReservationExpiresAt() != null && now.isAfter(listing.getReservationExpiresAt())) {
                throw new DomainException(EconomyError.LISTING_RESERVATION_EXPIRED);
            }
            if (!buyerId.equals(listing.getReservedBy())) {
                throw new DomainException(EconomyError.LISTING_RESERVED_OTHER);
            }
            if (!Objects.equals(command.reservationToken(), listing.getReservationToken())) {
                throw new DomainException(EconomyError.INVALID_RESERVATION_TOKEN);
            }
            buyerWallet.commitHold(listing.getReservedHoldId());
        } else if (listing.getStatus() == ListingStatus.OPEN) {
            buyerWallet.withdraw(listing.getPrice());
        } else {
            throw new DomainException(EconomyError.LISTING_NOT_AVAILABLE);
        }

        Trade trade = listing.sellTo(buyerId, command.reservationToken());

        sellerWallet.deposit(trade.getSellerProceeds());

        listingWriter.create(listing);

        Trade savedTrade = tradeWriter.create(trade);

        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.LISTING_PURCHASED)
                        .listingId(listing.getId())
                        .tradeId(savedTrade.getId())
                        .actorId(buyerId)
                        .occurredAt(now)
                        .build()
        );

        return EconomyResult.TradeSummary.from(savedTrade);
    }

    @Transactional
    public void cancel(Long sellerId, EconomyCommand.CancelListing command) {
        Listing listing = listingReader.getForUpdate(command.listingId());
        if (!sellerId.equals(listing.getSellerPlayerId())) {
            throw new DomainException(EconomyError.LISTING_NOT_AVAILABLE);
        }
        if (listing.getStatus() == ListingStatus.RESERVED && listing.getReservedHoldId() != null) {
            walletHoldReader.findByHoldId(listing.getReservedHoldId())
                    .ifPresent(hold -> handleHoldCancellation(hold, listing.getReservedHoldId()));
        }

        listing.cancel(sellerId);

        listingWriter.create(listing);

        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.LISTING_CANCELED)
                        .listingId(listing.getId())
                        .actorId(sellerId)
                        .occurredAt(Instant.now())
                        .build()
        );
    }

    @Transactional
    public void expireReservations() {
        Instant now = Instant.now();
        List<Listing> listings = listingReader.findReservedExpiringBefore(now);

        for (Listing listing : listings) {
            listing.expire(now);

            if (listing.getReservedHoldId() != null) {
                walletHoldReader.findByHoldId(listing.getReservedHoldId())
                        .ifPresent(hold -> handleHoldExpiry(hold, now));
            }

            listingWriter.create(listing);

            domainEventPublisher.publish(
                    EconomyEvent.builder(EconomyEventType.LISTING_EXPIRED)
                            .listingId(listing.getId())
                            .occurredAt(now)
                            .build()
            );
        }
    }

    @Transactional(readOnly = true)
    public EconomyResult.ListingSummaries listOpen() {
        List<Listing> listings = listingReader.listOpen();
        return EconomyResult.ListingSummaries.fromList(listings);
    }

    @Transactional(readOnly = true)
    public EconomyResult.PlayerListings listBySeller(Long sellerId) {
        List<Listing> listings = listingReader.listBySeller(sellerId);
        return EconomyResult.PlayerListings.fromList(listings);
    }

    @Transactional(readOnly = true)
    public EconomyResult.PlayerReservations listReservations(Long buyerId) {
        List<Listing> reservations = listingReader.listByReservedBy(buyerId);
        return EconomyResult.PlayerReservations.fromList(reservations);
    }

    @Transactional(readOnly = true)
    public EconomyResult.Trades listTrades(Long playerId) {
        List<Trade> trades = tradeReader.findByPlayer(playerId);
        return EconomyResult.Trades.fromList(trades);
    }

    private void handleHoldCancellation(WalletHold hold, String holdId) {
        Wallet wallet = lockOrCreateWallet(hold.getWallet().getOwnerId());
        wallet.cancelHold(holdId);
    }

    private void handleHoldExpiry(WalletHold hold, Instant now) {
        Wallet wallet = lockOrCreateWallet(hold.getWallet().getOwnerId());
        wallet.expireHolds(now);
    }

    private Wallet lockOrCreateWallet(Long ownerId) {
        return walletReader.getByOwnerIdForUpdate(ownerId)
                .orElseGet(() -> walletWriter.save(Wallet.open(ownerId)));
    }
}
