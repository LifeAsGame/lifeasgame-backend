package online.lifeasgame.inventory.application.model;

import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.domain.InstanceAttrs;

public final class InventorySpec {

    private InventorySpec() {}

    public record Add(
            int quantity,
            InstanceAttrs instanceAttrs,
            boolean bound
    ) {
        public static Add from(InventoryCommand.Add command) {
            return new Add(
                    command.quantity(),
                    InstanceAttrs.of(command.instanceAttrs()),
                    command.bound()
            );
        }

        public static Add of(int quantity, InstanceAttrs attrs, boolean bound) {
            return new Add(quantity, attrs, bound);
        }
    }
}
