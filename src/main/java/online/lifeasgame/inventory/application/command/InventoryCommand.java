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
        public static Add of(
                Long itemId,
                int quantity,
                Map<String, Object> attrs,
                boolean bound
        ) {
            return new Add(itemId, quantity, attrs, bound);
        }
    }

    public record Remove(int slotIndex, int quantity) {
        public static Remove of(int slotIndex, int quantity) {
            return new Remove(slotIndex, quantity);
        }
    }

    public record Move(int from, int to) {
        public static Move of(int from, int to) {
            return new Move(from, to);
        }
    }

    public record Merge(int from, int to) {
        public static Merge of(int from, int to) {
            return new Merge(from, to);
        }
    }

    public record Split(
            int from,
            Integer to,
            int quantity
    ) {
        public static Split of(
                int from,
                Integer to,
                int quantity
        ) {
            return new Split(from, to, quantity);
        }
    }
}
