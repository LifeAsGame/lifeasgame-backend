package online.lifeasgame.inventory.api.player.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import online.lifeasgame.inventory.domain.PlayerMailbox;

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
            @NotEmpty
            @Size(max = PlayerMailbox.DEFAULT_CAPACITY)
            List<@NotNull @Valid Claim> claims
    ) {}

    public record Delete(
            @Min(0) int slotIndex,
            String idempotencyKey
    ) {}
}
