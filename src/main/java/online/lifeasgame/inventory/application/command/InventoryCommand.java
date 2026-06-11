package online.lifeasgame.inventory.application.command;

import java.util.Map;

public final class InventoryCommand {

    private InventoryCommand() {
    }

    public record Add(
            Long itemId,
            int quantity,
            Map<String, Object> instanceAttrs,
            boolean bound
    ) {
    }

    public record Remove(int slotIndex, int quantity) {
    }

    public record Move(int from, int to) {
    }

    public record Merge(int from, int to) {
    }

    public record Split(
            int from,
            Integer to,
            int quantity
    ) {
    }
}
