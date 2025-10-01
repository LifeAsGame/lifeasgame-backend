package online.lifeasgame.inventory.api.player.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import online.lifeasgame.inventory.domain.InstanceAttrs;

public final class MailboxRequest {

    private MailboxRequest() {
    }

    public record Deliver(
            @NotNull Long itemId,
            @Min(1) int quantity,
            InstanceAttrs instanceAttrs,
            boolean bound
    ) {
    }

    public record Claim(
            @Min(0) int slotIndex,
            @Min(1) int quantity
    ) {
    }
}
