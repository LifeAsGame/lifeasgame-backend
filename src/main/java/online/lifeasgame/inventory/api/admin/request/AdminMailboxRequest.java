package online.lifeasgame.inventory.api.admin.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public final class AdminMailboxRequest {

    private AdminMailboxRequest() {}

    public record Deliver(
            @NotNull Long itemId,
            @Min(1) int quantity,
            Map<String, Object> instanceAttrs,
            boolean bound
    ) {
    }

    public record Delete(
            @Min(0) int slotIndex,
            String reason
    ) {}
}
