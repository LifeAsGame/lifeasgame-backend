package online.lifeasgame.inventory.api.admin.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class AdminMailboxRequest {

    private AdminMailboxRequest() {}

    public record Deliver(
            @NotNull @Positive Long itemId,
            @Min(1) int quantity,
            boolean bound,
            @NotBlank
            @Size(max = 512)
            @Pattern(regexp = "(?=.*[^\\p{Cf}\\p{Zs}])[^\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]*")
            String reason
    ) {
    }

    public record Delete(
            @Min(0) int slotIndex,
            String reason
    ) {}
}
