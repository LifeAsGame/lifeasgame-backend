package online.lifeasgame.economy.api.player.mapper;

import online.lifeasgame.economy.api.player.request.EconomyRequest;
import online.lifeasgame.economy.api.player.response.EconomyResponse;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.Currency;

public final class EconomyWebMapper {
    private EconomyWebMapper() {}

    public static EconomyCommand.OpenListing toCommand(EconomyRequest.OpenListing request) {
        Currency currency = Currency.parseOptional(request.currency(), Currency.GOLD);
        return EconomyCommand.OpenListing.of(request.itemInstanceId(), request.itemId(), request.price(), currency);
    }

    public static EconomyCommand.ReserveListing toCommand(Long listingId, EconomyRequest.ReserveListing request) {
        return EconomyCommand.ReserveListing.of(listingId, request.ttlSeconds());
    }

    public static EconomyCommand.PurchaseListing toCommand(Long listingId, EconomyRequest.PurchaseListing request) {
        return EconomyCommand.PurchaseListing.of(listingId, request.reservationToken(), request.idempotencyKey());
    }

    public static EconomyCommand.CancelListing toCommand(Long listingId) {
        return EconomyCommand.CancelListing.of(listingId);
    }

    public static EconomyCommand.PurchaseShopItem toCommand(EconomyRequest.PurchaseShopItem request) {
        return EconomyCommand.PurchaseShopItem.of(request.shopItemId(), request.quantity(), request.reserveOnly(), request.idempotencyKey());
    }

    public static EconomyCommand.ConfirmShopReservation toCommand(EconomyRequest.ConfirmShopReservation request) {
        return EconomyCommand.ConfirmShopReservation.of(request.reservationToken());
    }

    public static EconomyCommand.TopUp toCommand(EconomyRequest.TopUp request) {
        Currency currency = Currency.parseOptional(request.currency(), Currency.GOLD);
        return EconomyCommand.TopUp.of(request.amount(), currency, request.paymentKey(), request.orderId(), request.idempotencyKey());
    }

    public static EconomyResponse.ListingId toResponse(EconomyResult.ListingId result) {
        return new EconomyResponse.ListingId(result.id());
    }

    public static EconomyResponse.Reservation toResponse(EconomyResult.Reservation reservation) {
        return new EconomyResponse.Reservation(reservation.reservationToken(), reservation.holdId(), reservation.expiresAt());
    }

    public static EconomyResponse.Trade toTrade(EconomyResult.TradeSummary trade) {
        return new EconomyResponse.Trade(trade.id(), trade.listingId(), trade.buyerId(), trade.sellerId(), trade.price(), trade.currency());
    }

    public static EconomyResponse.ShopPurchaseId toResponse(EconomyResult.ShopPurchaseId id) {
        return new EconomyResponse.ShopPurchaseId(id.id());
    }

    public static EconomyResponse.ShopReservation toResponse(EconomyResult.ShopReservation reservation) {
        return new EconomyResponse.ShopReservation(reservation.reservationToken(), reservation.expiresAt());
    }

    public static EconomyResponse.Listings toResponse(EconomyResult.Listings listings) {
        return new EconomyResponse.Listings(listings.listings().stream().map(EconomyWebMapper::toResponse).toList());
    }

    public static EconomyResponse.PlayerListings toResponse(EconomyResult.PlayerListings listings) {
        return new EconomyResponse.PlayerListings(listings.listings().stream().map(EconomyWebMapper::toResponse).toList());
    }

    public static EconomyResponse.PlayerReservations toResponse(EconomyResult.PlayerReservations reservations) {
        return new EconomyResponse.PlayerReservations(reservations.reservations().stream().map(EconomyWebMapper::toResponse).toList());
    }

    public static EconomyResponse.ListingSummary toResponse(EconomyResult.ListingSummary summary) {
        return new EconomyResponse.ListingSummary(
                summary.id(),
                summary.itemId(),
                summary.sellerId(),
                summary.price(),
                summary.currency(),
                summary.status()
        );
    }

    public static EconomyResponse.ListingReservation toResponse(EconomyResult.ListingReservation reservation) {
        return new EconomyResponse.ListingReservation(
                reservation.listingId(),
                reservation.itemId(),
                reservation.price(),
                reservation.currency(),
                reservation.expiresAt()
        );
    }

    public static EconomyResponse.ShopItems toResponse(EconomyResult.ShopItems items) {
        return new EconomyResponse.ShopItems(items.items().stream().map(EconomyWebMapper::toResponse).toList());
    }

    public static EconomyResponse.ShopItem toResponse(EconomyResult.ShopItemView view) {
        return new EconomyResponse.ShopItem(
                view.id(),
                view.itemId(),
                view.price(),
                view.currency(),
                view.available(),
                view.globalStockLimit(),
                view.perPlayerLimit(),
                view.reservationTtlSec()
        );
    }

    public static EconomyResponse.ShopPurchases toResponse(EconomyResult.ShopPurchases purchases) {
        return new EconomyResponse.ShopPurchases(purchases.purchases().stream().map(EconomyWebMapper::toResponse).toList());
    }

    public static EconomyResponse.ShopPurchaseSummary toResponse(EconomyResult.ShopPurchaseView view) {
        return new EconomyResponse.ShopPurchaseSummary(
                view.id(),
                view.shopItemId(),
                view.quantity(),
                view.status(),
                view.reservationToken(),
                view.reservationExpiresAt()
        );
    }

    public static EconomyResponse.Trades toResponse(EconomyResult.Trades trades) {
        return new EconomyResponse.Trades(trades.trades().stream().map(EconomyWebMapper::toTradeSummary).toList());
    }

    public static EconomyResponse.TradeSummary toTradeSummary(EconomyResult.TradeSummary summary) {
        return new EconomyResponse.TradeSummary(
                summary.id(),
                summary.listingId(),
                summary.buyerId(),
                summary.sellerId(),
                summary.price(),
                summary.currency()
        );
    }

    public static EconomyResponse.WalletBalance toResponse(EconomyResult.WalletBalance balance) {
        return new EconomyResponse.WalletBalance(balance.amount(), balance.currency());
    }
}
