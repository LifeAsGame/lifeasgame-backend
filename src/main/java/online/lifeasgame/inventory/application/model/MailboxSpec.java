package online.lifeasgame.inventory.application.model;

import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.domain.InstanceAttrs;

public final class MailboxSpec {

    private MailboxSpec() {}

    public record Deliver(Long itemId, int quantity, InstanceAttrs instanceAttrs, boolean bound) {
        public static Deliver of(Long itemId, int quantity, InstanceAttrs attrs, boolean bound) {
            return new Deliver(itemId, quantity, attrs, bound);
        }

        public static Deliver from(MailboxCommand.Deliver command) {
            return new Deliver(
                    command.itemId(),
                    command.quantity(),
                    command.instanceAttrs(),
                    command.bound()
            );
        }
    }

    public record Claim(int slotIndex, int quantity, Long itemId) {
        public static Claim of(int slotIndex, int quantity, Long itemId) {
            return new Claim(slotIndex, quantity, itemId);
        }

        public static Claim from(MailboxCommand.Claim command) {
            return new Claim(
                    command.slotIndex(),
                    command.quantity(),
                    command.itemId()
            );
        }
    }

}
