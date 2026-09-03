package online.lifeasgame.economy.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum EconomyError implements ErrorCode {
    WALLET_NOT_FOUND("ECON-WALLET-NOT-FOUND", "Wallet not found", 404),
    LISTING_NOT_FOUND("ECON-LISTING-NOT-FOUND", "Listing not found", 404),
    LISTING_NOT_AVAILABLE("ECON-LISTING-NOT-AVAILABLE", "Listing not available", 400),
    LISTING_RESERVATION_EXPIRED("ECON-LISTING-RESERVATION-EXPIRED", "Listing reservation expired", 410),
    LISTING_RESERVED_OTHER("ECON-LISTING-RESERVED-OTHER", "Listing reserved by other buyer", 409),
    LISTING_ACTIVE_RESERVATION("ECON-LISTING-ACTIVE-RESERVATION", "Listing has an active reservation", 409),
    SHOP_ITEM_NOT_FOUND("ECON-SHOP-ITEM-NOT-FOUND", "Shop item not found", 404),
    SHOP_ITEM_DISABLED("ECON-SHOP-ITEM-DISABLED", "Shop item unavailable", 400),
    SHOP_STOCK_EXHAUSTED("ECON-SHOP-STOCK-EXHAUSTED", "Shop item is sold out", 409),
    SHOP_PER_PLAYER_LIMIT("ECON-SHOP-PER-PLAYER-LIMIT", "Per-player purchase limit exceeded", 409),
    DUPLICATE_REQUEST("ECON-DUPLICATE-REQUEST", "Duplicate request detected", 409),
    INVALID_IDEMPOTENCY_KEY("ECON-INVALID-IDEMPOTENCY-KEY", "Invalid idempotency key", 400),
    IDEMPOTENCY_PAYLOAD_CONFLICT("ECON-IDEMPOTENCY-PAYLOAD-CONFLICT", "Idempotency key payload conflict", 409),
    PAYMENT_REJECTED("ECON-PAYMENT-REJECTED", "Payment gateway rejected", 402),
    HOLD_NOT_FOUND("ECON-HOLD-NOT-FOUND", "Hold not found", 404),
    CANNOT_PURCHASE_OWN_LISTING("ECON-CANNOT-PURCHASE-OWN", "Cannot purchase own listing", 400),
    INVALID_RESERVATION_TOKEN("ECON-INVALID-RESERVATION-TOKEN", "Invalid reservation token", 400),
    INVALID_CURRENCY("ECON-INVALID-CURRENCY", "Invalid currency", 400),
    INVALID_LISTING_STATUS("ECON-INVALID-LISTING-STATUS", "Invalid listing status", 400),
    ;

    private final String code;
    private final String message;
    private final int status;

    EconomyError(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int status() {
        return status;
    }
}
