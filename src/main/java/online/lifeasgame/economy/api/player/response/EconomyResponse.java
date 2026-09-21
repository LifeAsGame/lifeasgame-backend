package online.lifeasgame.economy.api.player.response;

import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(name = "PlayerWalletBalance")
    public record WalletBalance(
            @Schema(description = "기존 호환 필드: GOLD 사용 가능액 (GOLD available과 동일)", example = "80")
            long amount,
            @Schema(description = "기존 호환 필드: 항상 GOLD", example = "GOLD")
            String currency,
            @Schema(description = "항상 GOLD, GEM 순서의 두 항목. 지갑 또는 해당 balance가 없으면 available은 0, OPEN hold가 없으면 held는 0")
            List<CurrencyBalance> balances
    ) {
    }

    @Schema(name = "PlayerWalletCurrencyBalance")
    public record CurrencyBalance(
            @Schema(allowableValues = {"GOLD", "GEM"}, example = "GOLD") String currency,
            @Schema(description = "저장된 사용 가능액. hold 설정 시 이미 차감되므로 held를 다시 차감하지 않음", example = "80")
            long available,
            @Schema(description = "미정산 OPEN hold 금액 합계. TTL 경과만으로 제외하지 않으며 확정·취소·만료 정리 후 반영", example = "20")
            long held
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
