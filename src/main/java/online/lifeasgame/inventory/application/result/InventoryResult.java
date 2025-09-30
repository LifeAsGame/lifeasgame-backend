package online.lifeasgame.inventory.application.result;

import online.lifeasgame.inventory.domain.InventoryEntry;
import online.lifeasgame.inventory.domain.SlotIndex;

import java.util.List;

public final class InventoryResult {

    private InventoryResult() {}

    public record Slots(List<Integer> slots) {
        public static Slots fromList(List<SlotIndex> slots) {
            return new Slots(slots.stream().map(SlotIndex::value).toList());
        }
    }

    public record Slot(int slot) {
        public static Slot of(int s) {
            return new Slot(s);
        }
    }

    public record Entry(int slotIndex, Long itemId, String rarity, int quantity, boolean bound) {
        public static Entry from(InventoryEntry e) {
            return new Entry(
                    e.getSlotIndex().value(),
                    e.getItemId(),
                    e.getRarity().name(),
                    e.getQuantity().value(),
                    e.isBound()
            );
        }
    }

    public record Entries(List<Entry> entries) {
        public static Entries of(List<Entry> list) {
            return new Entries(list);
        }

        public static Entries fromList(List<InventoryEntry> entries) {
            return new Entries(
                    entries.stream()
                        .map(Entry::from)
                        .toList()
            );
        }
    }
}
