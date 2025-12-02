package online.lifeasgame.economy.application.command;

import online.lifeasgame.economy.domain.Currency;

public final class EconomyCommand {
    private EconomyCommand() {}

    public record TopUp(long amount, Currency currency, String paymentKey, String orderId, String idempotencyKey) {
        public static TopUp of(long amount, Currency currency, String paymentKey, String orderId, String idempotencyKey) {
            return new TopUp(amount, currency, paymentKey, orderId, idempotencyKey);
        }
    }

    public record OpenListing(Long itemInstanceId, Long itemId, long price, Currency currency) {
        public static OpenListing of(Long itemInstanceId, Long itemId, long price, Currency currency) {
            return new OpenListing(itemInstanceId, itemId, price, currency);
        }
    }

    public record ReserveListing(Long listingId, int ttlSeconds) {
        public static ReserveListing of(Long listingId, int ttlSeconds) {
            return new ReserveListing(listingId, ttlSeconds);
        }
    }

    public record PurchaseListing(Long listingId, String reservationToken, String idempotencyKey) {
        public static PurchaseListing of(Long listingId, String reservationToken, String idempotencyKey) {
            return new PurchaseListing(listingId, reservationToken, idempotencyKey);
        }
    }

    public record CancelListing(Long listingId) {
        public static CancelListing of(Long listingId) { return new CancelListing(listingId); }
    }

    public record CreateShopItem(Long itemId, long price, Currency currency, Integer globalLimit, Integer perPlayerLimit,
                                 Integer reservationTtlSeconds) {
        public static CreateShopItem of(Long itemId, long price, Currency currency, Integer globalLimit, Integer perPlayerLimit,
                                        Integer reservationTtlSeconds) {
            return new CreateShopItem(itemId, price, currency, globalLimit, perPlayerLimit, reservationTtlSeconds);
        }
    }

    public record PurchaseShopItem(Long shopItemId, int quantity, boolean reserveOnly, String idempotencyKey) {
        public static PurchaseShopItem of(Long shopItemId, int quantity, boolean reserveOnly, String idempotencyKey) {
            return new PurchaseShopItem(shopItemId, quantity, reserveOnly, idempotencyKey);
        }
    }

    public record ConfirmShopReservation(String reservationToken) {
        public static ConfirmShopReservation of(String reservationToken) {
            return new ConfirmShopReservation(reservationToken);
        }
    }

    public record ToggleShopItem(Long shopItemId, boolean enabled) {
        public static ToggleShopItem of(Long shopItemId, boolean enabled) { return new ToggleShopItem(shopItemId, enabled); }
    }

    public record UpdateShopItem(Long shopItemId, Integer globalLimit, Integer perPlayerLimit, Integer reservationTtlSeconds) {
        public static UpdateShopItem of(Long shopItemId, Integer globalLimit, Integer perPlayerLimit, Integer reservationTtlSeconds) {
            return new UpdateShopItem(shopItemId, globalLimit, perPlayerLimit, reservationTtlSeconds);
        }
    }

    public record AdjustWallet(Long playerId, long amount, Currency currency, boolean debit, String reason) {
        public static AdjustWallet of(Long playerId, long amount, Currency currency, boolean debit, String reason) {
            return new AdjustWallet(playerId, amount, currency, debit, reason);
        }
    }
}
