package online.lifeasgame.economy.api.admin.response;

public final class AdminEconomyResponse {
    private AdminEconomyResponse() {}

    public record ShopItem(Long id, Long itemId, long price, String currency, boolean available,
                           Integer globalStockLimit, Integer perPlayerLimit, Integer reservationTtlSec) {}

    public record ShopItems(java.util.List<ShopItem> items) {}

    public record ShopPurchaseSummary(Long id, Long shopItemId, Long playerId, Integer quantity, String status,
                                      String reservationToken, java.time.Instant reservationExpiresAt) {}

    public record ShopPurchases(java.util.List<ShopPurchaseSummary> purchases) {}

    public record WalletBalance(long amount, String currency) {}
}
