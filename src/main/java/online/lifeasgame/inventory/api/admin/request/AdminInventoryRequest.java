package online.lifeasgame.inventory.api.admin.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public final class AdminInventoryRequest {

    private AdminInventoryRequest() {}

    public record Add(
            @NotNull Long itemId,
            @Min(1) int quantity,
            Map<String, Object> instanceAttrs,
            boolean bound
    ) {
    }
}
