package online.lifeasgame.inventory.api.admin.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import online.lifeasgame.inventory.domain.InstanceAttrs;

public final class AdminMailboxRequest {

    private AdminMailboxRequest() {}

    public record Deliver(
            @NotNull Long itemId,
            @Min(1) int quantity,
            InstanceAttrs instanceAttrs,
            boolean bound
    ) {
    }
}
