package online.lifeasgame.economy.api.admin.response;

import java.time.Instant;
import java.util.List;

public final class AdminEconomyResponse {

    private AdminEconomyResponse() {
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
            Long playerId,
            Integer quantity,
            String status,
            String reservationToken,
            Instant reservationExpiresAt
    ) {
    }

    public record ShopPurchases(List<ShopPurchaseSummary> purchases) {
    }

    public record WalletBalance(long amount, String currency) {
    }
}
