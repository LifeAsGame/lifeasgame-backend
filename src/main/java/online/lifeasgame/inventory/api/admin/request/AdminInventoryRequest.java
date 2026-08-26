package online.lifeasgame.inventory.api.admin.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class AdminInventoryRequest {

    private AdminInventoryRequest() {}

    public record Add(
            @NotNull @Positive Long itemId,
            @Min(1) int quantity,
            boolean bound,
            @NotBlank
            @Size(max = 512)
            @Pattern(regexp = "(?=.*[^\\p{Cf}\\p{Zs}])[^\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]*")
            String reason
    ) {

        /** Rejects the removed legacy input; this is not a business field. */
        @JsonAnySetter
        public void rejectLegacyInstanceAttrs(String name, Object value) {
            if ("instanceAttrs".equals(name) && value != null) {
                throw new IllegalArgumentException(
                        "instanceAttrs is not supported"
                );
            }
        }
    }

    public record SetCapacity(
            @Min(1) int capacitySlots
    ) {}
}
