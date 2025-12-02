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

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final ListingReader listingReader;
    private final ListingWriter listingWriter;
    private final TradeWriter tradeWriter;
    private final TradeReader tradeReader;
    private final WalletWriter walletWriter;
    private final WalletHoldReader walletHoldReader;
    private final IdempotencyKeyStore idempotencyKeyStore;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public EconomyResult.ListingId open(Long sellerId, EconomyCommand.OpenListing command) {
        Money price = Money.of(command.price(), command.currency());
        Listing saved = listingWriter.create(sellerId, command.itemInstanceId(), command.itemId(), price);
        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.LISTING_OPENED)
                        .listingId(saved.getId())
                        .actorId(sellerId)
                        .occurredAt(Instant.now())
                        .build()
        );
        return EconomyResult.ListingId.of(saved.getId());
    }

    @Transactional
    public EconomyResult.Reservation reserve(Long buyerId, EconomyCommand.ReserveListing command) {
        Listing listing = listingReader.getForUpdate(command.listingId());
        Instant now = Instant.now();
        listing.expire(now);
        if (listing.getStatus() != ListingStatus.OPEN) {
            throw new DomainException(EconomyError.LISTING_NOT_AVAILABLE);
        }
        Wallet wallet = walletWriter.getOrCreateForUpdate(buyerId);
        String holdId = wallet.placeHold(listing.getPrice(), "listing-reserve-" + listing.getId(), now, command.ttlSeconds());
        ReservationToken token = listing.reserve(buyerId, holdId, now, command.ttlSeconds());
        walletWriter.save(wallet);
        listingWriter.save(listing);
        domainEventPublisher.publish(
                EconomyEvent.builder(EconomyEventType.LISTING_RESERVED)
                        .listingId(listing.getId())
                        .actorId(buyerId)
                        .reservationToken(token.value())
                        .occurredAt(now)
                        .build()
        );
        return EconomyResult.Reservation.of(token.value(), holdId, listing.getReservationExpiresAt());
    }

    @Transactional
    public EconomyResult.TradeSummary purchase(Long buyerId, EconomyCommand.PurchaseListing command) {
        if (!idempotencyKeyStore.acquire(command.idempotencyKey(), Duration.ofMinutes(5))) {
            throw new DomainException(EconomyError.DUPLICATE_REQUEST);
        }

        Listing listing = listingReader.getForUpdate(command.listingId());
        Instant now = Instant.now();
        listing.expire(now);
        Wallet buyerWallet = walletWriter.getOrCreateForUpdate(buyerId);
        Wallet sellerWallet = walletWriter.getOrCreateForUpdate(listing.getSellerPlayerId());

        if (listing.getStatus() == ListingStatus.RESERVED) {
            if (listing.getReservationExpiresAt() != null && now.isAfter(listing.getReservationExpiresAt())) {
                throw new DomainException(EconomyError.LISTING_RESERVATION_EXPIRED);
            }
            if (!buyerId.equals(listing.getReservedBy())) {
                throw new DomainException(EconomyError.LISTING_RESERVED_OTHER);
            }
            buyerWallet.commitHold(listing.getReservedHoldId());
        } else if (listing.getStatus() == ListingStatus.OPEN) {
            buyerWallet.withdraw(listing.getPrice());
        } else {
            throw new DomainException(EconomyError.LISTING_NOT_AVAILABLE);
        }

        Trade trade = listing.sellTo(buyerId, command.reservationToken());
        sellerWallet.deposit(trade.getSellerProceeds());

        walletWriter.save(buyerWallet);
        walletWriter.save(sellerWallet);
        listingWriter.save(listing);

        Trade savedTrade = tradeWriter.save(trade);
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
        listingWriter.save(listing);
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
            listingWriter.save(listing);
            domainEventPublisher.publish(
                    EconomyEvent.builder(EconomyEventType.LISTING_EXPIRED)
                            .listingId(listing.getId())
                            .occurredAt(now)
                            .build()
            );
        }
    }

    @Transactional(readOnly = true)
    public EconomyResult.Listings listOpen() {
        List<Listing> listings = listingReader.listOpen();
        return new EconomyResult.Listings(listings.stream().map(EconomyResult.ListingSummary::from).toList());
    }

    @Transactional(readOnly = true)
    public EconomyResult.PlayerListings listBySeller(Long sellerId) {
        List<Listing> listings = listingReader.listBySeller(sellerId);
        return new EconomyResult.PlayerListings(listings.stream().map(EconomyResult.ListingSummary::from).toList());
    }

    @Transactional(readOnly = true)
    public EconomyResult.PlayerReservations listReservations(Long buyerId) {
        List<Listing> reservations = listingReader.listByReservedBy(buyerId);
        return new EconomyResult.PlayerReservations(reservations.stream().map(EconomyResult.ListingReservation::from).toList());
    }

    @Transactional(readOnly = true)
    public EconomyResult.Trades listTrades(Long playerId) {
        return new EconomyResult.Trades(tradeReader.findByPlayer(playerId).stream()
                .map(EconomyResult.TradeSummary::from)
                .toList());
    }

    private void handleHoldCancellation(WalletHold hold, String holdId) {
        hold.getWallet().cancelHold(holdId);
        walletWriter.save(hold.getWallet());
    }

    private void handleHoldExpiry(WalletHold hold, Instant now) {
        hold.getWallet().expireHolds(now);
        walletWriter.save(hold.getWallet());
    }
}
