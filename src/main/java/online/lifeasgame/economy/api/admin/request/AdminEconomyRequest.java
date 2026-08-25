package online.lifeasgame.economy.api.admin.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminEconomyRequest {

    private AdminEconomyRequest() {
    }

    public record CreateShopItem(
            @NotNull Long itemId,
            @Min(1) long price,
            @NotBlank String currency,
            Integer globalLimit,
            Integer perPlayerLimit,
            Integer reservationTtlSeconds
    ) {
    }

    public record ToggleShopItem(
            @NotNull Boolean enabled
    ) {
    }

    public record UpdateShopItem(
            Integer globalLimit,
            Integer perPlayerLimit,
            Integer reservationTtlSeconds
    ) {
    }

    public record AdjustWallet(
            @Min(1) long amount,
            @NotBlank String currency,
            boolean debit,
            @NotBlank
            @Size(max = 512)
            @Pattern(regexp = "[^\\p{Cc}\\p{Zl}\\p{Zp}]*")
            String reason
    ) {
    }
}
