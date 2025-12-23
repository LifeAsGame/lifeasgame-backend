package online.lifeasgame.inventory.application.result;

import online.lifeasgame.inventory.domain.InventoryEntry;
import online.lifeasgame.inventory.domain.SlotIndex;

import java.util.List;

public final class InventoryResult {

    private InventoryResult() {}

    public record Slots(List<Integer> slots) {
        public static Slots fromList(List<SlotIndex> slots) {
            return new Slots(
                    slots.stream()
                            .map(SlotIndex::value)
                            .toList()
            );
        }
    }

    public record Slot(int slot) {
    }

    public record Entry(
            int slotIndex,
            Long itemId,
            String rarity,
            int quantity,
            boolean bound
    ) {
        public static Entry from(InventoryEntry inventoryEntry) {
            return new Entry(
                    inventoryEntry.getSlotIndex().value(),
                    inventoryEntry.getItemId(),
                    inventoryEntry.getRarity().name(),
                    inventoryEntry.getQuantity().value(),
                    inventoryEntry.isBound()
            );
        }
    }

    public record Entries(List<Entry> entries) {
        public static Entries fromList(List<InventoryEntry> entries) {
            return new Entries(
                    entries.stream()
                            .map(Entry::from)
                            .toList()
            );
        }
    }
}
