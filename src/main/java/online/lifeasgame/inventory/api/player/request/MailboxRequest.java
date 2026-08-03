package online.lifeasgame.inventory.api.player.request;

import jakarta.validation.constraints.Min;

import java.util.List;

public final class MailboxRequest {

    private MailboxRequest() {
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
