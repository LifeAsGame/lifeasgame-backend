package online.lifeasgame.economy.api.player.response;

import java.time.Instant;

public final class EconomyResponse {
    private EconomyResponse() {}

    public record ListingId(Long id) {}

    public record Reservation(String reservationToken, String holdId, Instant expiresAt) {}

    public record Trade(Long id, Long listingId, Long buyerId, Long sellerId, long price, String currency) {}

    public record ShopPurchaseId(Long id) {}

    public record ShopReservation(String reservationToken, Instant expiresAt) {}

    public record ListingSummary(Long id, Long itemId, Long sellerId, long price, String currency, String status) {}

    public record ListingReservation(Long listingId, Long itemId, long price, String currency, Instant expiresAt) {}

    public record Listings(java.util.List<ListingSummary> listings) {}

    public record PlayerListings(java.util.List<ListingSummary> listings) {}

    public record PlayerReservations(java.util.List<ListingReservation> reservations) {}

    public record ShopItem(Long id, Long itemId, long price, String currency, boolean available,
                          Integer globalStockLimit, Integer perPlayerLimit, Integer reservationTtlSec) {}

    public record ShopItems(java.util.List<ShopItem> items) {}

    public record ShopPurchaseSummary(Long id, Long shopItemId, Integer quantity, String status,
                                      String reservationToken, Instant reservationExpiresAt) {}

    public record ShopPurchases(java.util.List<ShopPurchaseSummary> purchases) {}

    public record WalletBalance(long amount, String currency) {}

    public record TradeSummary(Long id, Long listingId, Long buyerId, Long sellerId, long price, String currency) {}

    public record Trades(java.util.List<TradeSummary> trades) {}
}
