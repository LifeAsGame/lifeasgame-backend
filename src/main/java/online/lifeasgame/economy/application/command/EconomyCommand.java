package online.lifeasgame.economy.application.command;

public final class EconomyCommand {

    private EconomyCommand() {
    }

    public record TopUp(
            long amount,
            String currency,
            String paymentKey,
            String orderId,
            String idempotencyKey
    ) {
    }

    public record OpenListing(
            Long itemInstanceId,
            Long itemId,
            long price,
            String currency
    ) {
    }

    public record ReserveListing(
            Long listingId,
            int ttlSeconds
    ) {
    }

    public record PurchaseListing(
            Long listingId,
            String reservationToken,
            String idempotencyKey
    ) {
    }

    public record CancelListing(Long listingId) {
    }

    public record CreateShopItem(
            Long itemId,
            long price,
            String currency,
            Integer globalLimit,
            Integer perPlayerLimit,
            Integer reservationTtlSeconds
    ) {
    }

    public record PurchaseShopItem(
            Long shopItemId,
            int quantity,
            boolean reserveOnly,
            String idempotencyKey
    ) {
    }

    public record ConfirmShopReservation(String reservationToken) {
    }

    public record ToggleShopItem(
            Long shopItemId,
            boolean enabled
    ) {
    }

    public record UpdateShopItem(
            Long shopItemId,
            Integer globalLimit,
            Integer perPlayerLimit,
            Integer reservationTtlSeconds
    ) {
    }

    public record AdjustWallet(
            Long playerId,
            long amount,
            String currency,
            boolean debit,
            String reason
    ) {
    }
}
