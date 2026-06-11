package online.lifeasgame.economy.api.player.response;

import java.time.Instant;
import java.util.List;

public final class EconomyResponse {

    private EconomyResponse() {
    }

    public record Page<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record ListingId(Long id) {
    }

    public record Reservation(
            String reservationToken,
            String holdId,
            Instant expiresAt
    ) {
    }

    public record ListingDetail(
            Long id,
            Long itemInstanceId,
            Long itemId,
            Long sellerId,
            long price,
            String currency,
            String status,
            Long reservedBy,
            Instant reservationExpiresAt
    ) {
    }

    public record Trade(
            Long id,
            Long listingId,
            Long buyerId,
            Long sellerId,
            long price,
            String currency
    ) {
    }

    public record ShopPurchaseId(Long id) {
    }

    public record ShopReservation(
            String reservationToken,
            Instant expiresAt
    ) {
    }

    public record ListingSummary(
            Long id,
            Long itemId,
            Long sellerId,
            long price,
            String currency,
            String status
    ) {
    }

    public record ListingReservation(
            Long listingId,
            Long itemId,
            long price,
            String currency,
            Instant expiresAt
    ) {
    }

    public record Listings(List<ListingSummary> listings) {
    }

    public record PlayerListings(List<ListingSummary> listings) {
    }

    public record PlayerReservations(List<ListingReservation> reservations) {
    }

    public record ShopItem(
            Long id,
            Long itemId,
            long price,
            String currency,
            boolean available,
            Integer globalStockLimit,
            Integer perPlayerLimit,
            Integer reservationTtlSec
    ) {
    }

    public record ShopItems(List<ShopItem> items) {
    }

    public record ShopPurchaseSummary(
            Long id,
            Long shopItemId,
            Integer quantity,
            String status,
            String reservationToken,
            Instant reservationExpiresAt
    ) {
    }

    public record ShopPurchases(List<ShopPurchaseSummary> purchases) {
    }

    public record WalletBalance(
            long amount,
            String currency
    ) {
    }

    public record TradeSummary(
            Long id,
            Long listingId,
            Long buyerId,
            Long sellerId,
            long price,
            String currency
    ) {
    }

    public record Trades(List<TradeSummary> trades) {
    }
}
