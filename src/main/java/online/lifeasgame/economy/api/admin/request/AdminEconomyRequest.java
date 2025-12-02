package online.lifeasgame.economy.api.admin.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class AdminEconomyRequest {
    private AdminEconomyRequest() {}

    public record CreateShopItem(
            @NotNull Long itemId,
            @Min(1) long price,
            @NotBlank String currency,
            Integer globalLimit,
            Integer perPlayerLimit,
            Integer reservationTtlSeconds
    ) {}

    public record ToggleShopItem(
            @NotNull Boolean enabled
    ) {}

    public record UpdateShopItem(
            Integer globalLimit,
            Integer perPlayerLimit,
            Integer reservationTtlSeconds
    ) {}

    public record AdjustWallet(
            @Min(1) long amount,
            @NotBlank String currency,
            boolean debit,
            String reason
    ) {}
}
