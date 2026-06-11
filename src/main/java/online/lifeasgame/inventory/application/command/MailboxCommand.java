package online.lifeasgame.inventory.application.command;

import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.Map;

public final class MailboxCommand {

    private MailboxCommand() {}

    public record Deliver(
            Long itemId,
            int quantity,
            Map<String, Object> instanceAttrs,
            boolean bound
    ) {
    }

    public record Claim(int slotIndex, int quantity) {
    }

    public record ClaimAll(
            List<Claim> claims
    ) {
    }

    public record Delete(
            @Min(0) int slotIndex
    ) {
    }
}
