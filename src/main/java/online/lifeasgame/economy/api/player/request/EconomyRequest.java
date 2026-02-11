package online.lifeasgame.economy.api.player.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class EconomyRequest {

    private EconomyRequest() {
    }

    public record OpenListing(
            @NotNull Long itemInstanceId,
            @NotNull Long itemId,
            @Min(1) long price,
            @NotBlank String currency
    ) {
    }

    public record ChangeListingPrice(
            @Min(1) long price,
            @NotBlank String currency
    ) {
    }

    public record ReserveListing(
            @Min(1) int ttlSeconds
    ) {
    }

    public record CancelReservation(
            @NotBlank String reservationToken
    ) {
    }

    public record PurchaseListing(
            String reservationToken,
            @NotBlank String idempotencyKey
    ) {
    }

    public record PurchaseShopItem(
            @NotNull Long shopItemId,
            @Min(1) int quantity,
            boolean reserveOnly,
            @NotBlank String idempotencyKey
    ) {
    }

    public record ConfirmShopReservation(
            @NotBlank String reservationToken,
            String idempotencyKey
    ) {
    }

    public record CancelShopReservation(
            @NotBlank String reservationToken,
            String idempotencyKey
    ) {
    }

    public record TopUp(
            @Min(1) long amount,
            @NotBlank String currency,
            @NotBlank String paymentKey,
            @NotBlank String orderId,
            @NotBlank String idempotencyKey
    ) {
    }
}
