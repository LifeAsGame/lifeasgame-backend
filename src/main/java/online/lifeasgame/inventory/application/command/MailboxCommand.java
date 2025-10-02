package online.lifeasgame.inventory.application.command;

import java.util.Map;

public final class MailboxCommand {

    private MailboxCommand() {}

    public record Deliver(
            Long itemId,
            int quantity,
            Map<String, Object> instanceAttrs,
            boolean bound
    ) {
        public static Deliver of(
                Long itemId,
                int quantity,
                Map<String, Object> instanceAttrs,
                boolean bound
        ) {
            return new Deliver(itemId, quantity, instanceAttrs, bound);
        }
    }

    public record Claim(int slotIndex, int quantity) {
        public static Claim of(int slotIndex, int quantity) {
            return new Claim(slotIndex, quantity);
        }
    }
}
