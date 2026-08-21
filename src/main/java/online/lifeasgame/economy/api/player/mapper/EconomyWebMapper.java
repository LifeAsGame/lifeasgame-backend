package online.lifeasgame.economy.api.player.mapper;

import online.lifeasgame.economy.api.player.request.EconomyRequest;
import online.lifeasgame.economy.api.player.response.EconomyResponse;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;

public final class EconomyWebMapper {

    private EconomyWebMapper() {
    }

    public static EconomyResponse.Listings toListings(EconomyResult.ListingSummaries result) {
        return new EconomyResponse.Listings(
                result.listings().stream()
                        .map(EconomyWebMapper::toListingSummary)
                        .toList()
        );
    }

    public static EconomyResponse.PlayerListings toPlayerListings(EconomyResult.PlayerListings result) {
        return new EconomyResponse.PlayerListings(
                result.listings().stream().
                        map(EconomyWebMapper::toListingSummary)
                        .toList()
        );
    }

    public static EconomyResponse.ListingSummary toListingSummary(EconomyResult.ListingSummary result) {
        return new EconomyResponse.ListingSummary(
                result.id(),
                result.itemId(),
                result.sellerId(),
                result.price(),
                result.currency(),
                result.status()
        );
    }

    public static EconomyResponse.PlayerReservations toPlayerReservations(EconomyResult.PlayerReservations result) {
        return new EconomyResponse.PlayerReservations(
                result.reservations().stream()
                        .map(EconomyWebMapper::toListingReservation)
                        .toList()
        );
    }

    public static EconomyResponse.ListingReservation toListingReservation(EconomyResult.ListingReservation result) {
        return new EconomyResponse.ListingReservation(
                result.listingId(),
                result.itemId(),
                result.price(),
                result.currency(),
                result.expiresAt()
        );
    }

    public static EconomyResponse.Trades toTrades(EconomyResult.Trades result) {
        return new EconomyResponse.Trades(
                result.trades().stream()
                        .map(EconomyWebMapper::toTradeSummary)
                        .toList()
        );
    }

    public static EconomyCommand.OpenListing toOpenListingCommand(EconomyRequest.OpenListing request) {
        return new EconomyCommand.OpenListing(
                request.inventoryEntryId(),
                request.price(),
                request.currency()
        );
    }

    public static EconomyResponse.ListingId toListingId(EconomyResult.ListingId result) {
        return new EconomyResponse.ListingId(result.id());
    }

    public static EconomyCommand.ReserveListing toReserveListingCommand(Long listingId, EconomyRequest.ReserveListing request) {
        return new EconomyCommand.ReserveListing(listingId, request.ttlSeconds());
    }

    public static EconomyResponse.Reservation toReservation(EconomyResult.Reservation result) {
        return new EconomyResponse.Reservation(result.reservationToken(), result.holdId(), result.expiresAt());
    }

    public static EconomyCommand.PurchaseListing toPurchaseListingCommand(Long listingId, EconomyRequest.PurchaseListing request) {
        return new EconomyCommand.PurchaseListing(
                listingId,
                request.reservationToken(),
                request.idempotencyKey()
        );
    }

    public static EconomyResponse.Trade toTrade(EconomyResult.TradeSummary result) {
        return new EconomyResponse.Trade(
                result.id(),
                result.listingId(),
                result.buyerId(),
                result.sellerId(),
                result.price(),
                result.currency()
        );
    }

    public static EconomyCommand.CancelListing toCancelListingCommand(Long listingId) {
        return new EconomyCommand.CancelListing(listingId);
    }

    public static EconomyResponse.TradeSummary toTradeSummary(EconomyResult.TradeSummary result) {
        return new EconomyResponse.TradeSummary(
                result.id(),
                result.listingId(),
                result.buyerId(),
                result.sellerId(),
                result.price(),
                result.currency()
        );
    }

    public static EconomyCommand.PurchaseShopItem toPurchaseShopItemCommand(EconomyRequest.PurchaseShopItem request) {
        return new EconomyCommand.PurchaseShopItem(
                request.shopItemId(),
                request.quantity(),
                request.reserveOnly(),
                request.idempotencyKey()
        );
    }

    public static EconomyResponse.ShopItems toShopItems(EconomyResult.ShopItems result) {
        return new EconomyResponse.ShopItems(
                result.items().stream()
                        .map(EconomyWebMapper::toShopItem)
                        .toList()
        );
    }

    public static EconomyResponse.ShopItem toShopItem(EconomyResult.ShopItemView result) {
        return new EconomyResponse.ShopItem(
                result.id(),
                result.itemId(),
                result.price(),
                result.currency(),
                result.available(),
                result.globalStockLimit(),
                result.perPlayerLimit(),
                result.reservationTtlSec()
        );
    }

    public static EconomyResponse.ShopPurchases toShopPurchases(EconomyResult.ShopPurchases result) {
        return new EconomyResponse.ShopPurchases(
                result.purchases().stream()
                        .map(EconomyWebMapper::toShopPurchaseSummary)
                        .toList()
        );
    }

    public static EconomyResponse.ShopPurchaseSummary toShopPurchaseSummary(EconomyResult.ShopPurchaseView result) {
        return new EconomyResponse.ShopPurchaseSummary(
                result.id(),
                result.shopItemId(),
                result.quantity(),
                result.status(),
                result.reservationToken(),
                result.reservationExpiresAt()
        );
    }

    public static EconomyResponse.ShopPurchaseId toShopPurchaseId(EconomyResult.ShopPurchaseId result) {
        return new EconomyResponse.ShopPurchaseId(result.id());
    }

    public static EconomyCommand.ConfirmShopReservation toConfirmShopReservationCommand(EconomyRequest.ConfirmShopReservation request) {
        return new EconomyCommand.ConfirmShopReservation(request.reservationToken());
    }

    public static EconomyResponse.ShopReservation toShopReservation(EconomyResult.ShopReservation result) {
        return new EconomyResponse.ShopReservation(result.reservationToken(), result.expiresAt());
    }

    public static EconomyResponse.WalletBalance toWalletBalance(EconomyResult.WalletBalance result) {
        return new EconomyResponse.WalletBalance(result.amount(), result.currency());
    }

    public static EconomyCommand.TopUp toTopUpCommand(EconomyRequest.TopUp request) {
        return new EconomyCommand.TopUp(
                request.amount(),
                request.currency(),
                request.paymentKey(),
                request.orderId(),
                request.idempotencyKey()
        );
    }
}
