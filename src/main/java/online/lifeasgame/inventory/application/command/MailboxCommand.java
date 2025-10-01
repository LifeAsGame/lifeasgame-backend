package online.lifeasgame.inventory.application.command;

import online.lifeasgame.inventory.domain.InstanceAttrs;

public final class MailboxCommand {

    private MailboxCommand() {}

    public record Deliver(
            Long itemId,
            int quantity,
            InstanceAttrs instanceAttrs,
            boolean bound
    ) {
        public static Deliver of(
                Long itemId, int quantity, InstanceAttrs attrs, boolean bound) {
            return new Deliver(itemId, quantity, attrs, bound);
        }
    }

    public record Claim(int slotIndex, int quantity) {
        public static Claim of(int slotIndex, int quantity) {
            return new Claim(slotIndex, quantity);
        }
    }
}
