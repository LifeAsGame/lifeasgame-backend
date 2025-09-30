package online.lifeasgame.inventory.api.player.response;

import java.util.List;

public final class InventoryResponse {

    private InventoryResponse() {}

    public record Slots(List<Integer> slots) {
        public static Slots of(List<Integer> s) {
            return new Slots(s);
        }
    }

    public record Slot(int slot) {
        public static Slot of(int s) {
            return new Slot(s);
        }
    }

    public record Entry(
            int slotIndex,
            Long itemId,
            String rarity,
            int quantity,
            boolean bound
    ) {
        public static Entry of(int slotIndex, Long itemId, String rarity, int quantity, boolean bound) {
            return new Entry(slotIndex, itemId, rarity, quantity, bound);
        }
    }

    public record Entries(List<Entry> entries) {
        public static Entries fromList(List<Entry> entries) {
            return new Entries(
                    entries.stream().map(e -> new InventoryResponse.Entry(
                            e.slotIndex(),
                            e.itemId(),
                            e.rarity(),
                            e.quantity(),
                            e.bound()
                    )).toList()
            );
        }
    }
}
