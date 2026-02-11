package online.lifeasgame.inventory.api.player.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public final class MailboxRequest {

    private MailboxRequest() {
    }

    public record Deliver(
            @NotNull Long itemId,
            @Min(1) int quantity,
            Map<String, Object> instanceAttrs,
            boolean bound
    ) {
    }

    public record Claim(
            @Min(0) int slotIndex,
            @Min(1) int quantity
    ) {
    }

    public record ClaimAll(
            List<Claim> claims
    ) {}

    public record Delete(
            @Min(0) int slotIndex,
            String idempotencyKey
    ) {}
}
