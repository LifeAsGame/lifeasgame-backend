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
import online.lifeasgame.inventory.application.internal.InventoryMarketAvailabilityApi;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final ListingReader listingReader;
    private final ListingWriter listingWriter;
    private final ListingReservationReader listingReservationReader;
    private final ListingReservationWriter listingReservationWriter;
    private final TradeWriter tradeWriter;
    private final TradeReader tradeReader;
    private final WalletWriter walletWriter;
    private final WalletReader walletReader;
    private final InventoryMarketAvailabilityApi inventoryMarketAvailabilityApi;
    private final IdempotencyKeyStore idempotencyKeyStore;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public EconomyResult.Reservation reserve(Long buyerId, EconomyCommand.ReserveListing command) {
        Listing listing = listingReader.getForUpdate(command.listingId());
        Instant now = Instant.now();

        if (listing.getStatus() != ListingStatus.OPEN) {
            throw new DomainException(EconomyError.LISTING_NOT_AVAILABLE);
        }
        if (buyerId.equals(listing.getSellerPlayerId())) {
            throw new DomainException(EconomyError.CANNOT_PURCHASE_OWN_LISTING);
        }
        if (listingReservationReader.findActiveForUpdate(listing.getId()).isPresent()) {
            throw new DomainException(EconomyError.LISTING_RESERVED_OTHER);
        }

        Wallet wallet = lockOrCreateWallet(buyerId);
        String holdId = wallet.placeHold(
                listing.getPrice(),
                "listing-reserve" + listing.getId(),
                now,
                command.ttlSeconds()
        );
        inventoryMarketAvailabilityApi.reserveForTrade(
                listing.getSellerPlayerId(),
                listing.getItemInstanceId()
        );
        ListingReservation reservation = listingReservationWriter.save(ListingReservation.active(
                listing.getId(),
                buyerId,
                holdId,
                now,
                command.ttlSeconds()
        ));

        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.LISTING_RESERVED)
                        .listingId(listing.getId())
                        .actorId(buyerId)
                        .reservationToken(reservation.getReservationToken())
                        .occurredAt(now)
                        .build()
        );

        return new EconomyResult.Reservation(
                reservation.getReservationToken(),
                holdId,
                reservation.getExpiresAt()
        );
    }

    @Transactional
    public EconomyResult.TradeSummary purchase(Long buyerId, EconomyCommand.PurchaseListing command) {
        if (!idempotencyKeyStore.acquire(command.idempotencyKey(), Duration.ofMinutes(5))) {
            throw new DomainException(EconomyError.DUPLICATE_REQUEST);
        }

        Listing listing = listingReader.getForUpdate(command.listingId());

        Instant now = Instant.now();
        if (buyerId.equals(listing.getSellerPlayerId())) {
            throw new DomainException(EconomyError.CANNOT_PURCHASE_OWN_LISTING);
        }

        var activeReservation = listingReservationReader.findActiveForUpdate(listing.getId());

        Wallet buyerWallet = activeReservation.isPresent()
                ? lockWallet(buyerId)
                : lockOrCreateWallet(buyerId);
        Wallet sellerWallet = lockOrCreateWallet(listing.getSellerPlayerId());

        if (activeReservation.isPresent()) {
            ListingReservation reservation = activeReservation.get();
            reservation.consume(buyerId, command.reservationToken(), now);
            buyerWallet.commitHold(reservation.getWalletHoldId());
            listingReservationWriter.save(reservation);
        } else if (listing.getStatus() == ListingStatus.RESERVED) {
            if (listing.getReservationExpiresAt() != null && now.isAfter(listing.getReservationExpiresAt())) {
                throw new DomainException(EconomyError.LISTING_RESERVATION_EXPIRED);
            }
            if (!buyerId.equals(listing.getReservedBy())) {
                throw new DomainException(EconomyError.LISTING_RESERVED_OTHER);
            }
            if (listing.getReservationToken() == null
                    || !listing.getReservationToken().value().equals(command.reservationToken())) {
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
        if (listing.getStatus() == ListingStatus.RESERVED
                || listingReservationReader.findActiveForUpdate(listing.getId()).isPresent()) {
            throw new DomainException(EconomyError.LISTING_ACTIVE_RESERVATION);
        }
        if (listing.getStatus() != ListingStatus.OPEN) {
            throw new DomainException(EconomyError.LISTING_NOT_AVAILABLE);
        }

        listing.cancel(sellerId);
        inventoryMarketAvailabilityApi.releaseListing(
                sellerId,
                listing.getItemInstanceId()
        );
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
        List<Long> listingIds = listingReservationReader.findActiveListingIdsExpiringBefore(now);

        for (Long listingId : listingIds) {
            Listing listing = listingReader.getForUpdate(listingId);
            var activeReservation = listingReservationReader.findActiveForUpdate(listingId);
            if (activeReservation.isEmpty() || !activeReservation.get().isExpiredAt(now)) {
                continue;
            }
            ListingReservation reservation = activeReservation.get();
            reservation.expire(now);
            Wallet wallet = lockWallet(reservation.getBuyerPlayerId());
            wallet.expireHolds(now);
            inventoryMarketAvailabilityApi.releaseTradeReservation(
                    listing.getSellerPlayerId(),
                    listing.getItemInstanceId()
            );
            listingReservationWriter.save(reservation);

            domainEventPublisher.publish(
                    EconomyEvent.builder(EconomyEventType.LISTING_RESERVATION_EXPIRED)
                            .listingId(listing.getId())
                            .actorId(reservation.getBuyerPlayerId())
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
        List<EconomyResult.ListingReservation> reservations = new ArrayList<>();
        // ponytail: this legacy unpaged endpoint uses one Listing lookup per active reservation;
        // batch when reservation list volume or pagination makes it measurable.
        for (ListingReservation reservation : listingReservationReader.listActiveByBuyer(buyerId)) {
            Listing listing = listingReader.get(reservation.getListingId());
            reservations.add(EconomyResult.ListingReservation.from(
                    listing,
                    reservation.getExpiresAt()
            ));
        }
        listingReader.listByReservedBy(buyerId).stream()
                .map(EconomyResult.ListingReservation::from)
                .forEach(reservations::add);
        return new EconomyResult.PlayerReservations(List.copyOf(reservations));
    }

    @Transactional(readOnly = true)
    public EconomyResult.Trades listTrades(Long playerId) {
        List<Trade> trades = tradeReader.findByPlayer(playerId);
        return EconomyResult.Trades.fromList(trades);
    }

    private Wallet lockOrCreateWallet(Long ownerId) {
        return walletReader.getByOwnerIdForUpdate(ownerId)
                .orElseGet(() -> walletWriter.save(Wallet.open(ownerId)));
    }

    private Wallet lockWallet(Long ownerId) {
        return walletReader.getByOwnerIdForUpdate(ownerId)
                .orElseThrow(() -> new DomainException(EconomyError.WALLET_NOT_FOUND));
    }
}
