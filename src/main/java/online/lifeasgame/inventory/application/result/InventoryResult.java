package online.lifeasgame.inventory.application.result;

import online.lifeasgame.inventory.application.query.InventoryEntryView;
import online.lifeasgame.inventory.domain.SlotIndex;

import java.util.List;
import java.util.Map;

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
            Long itemInstanceId,
            int slotIndex,
            Long itemId,
            String itemName,
            String category,
            String type,
            String rarity,
            boolean stackable,
            int maxStack,
            int quantity,
            boolean bound,
            Integer durability,
            Map<String, Object> instanceAttrs
    ) {
        public static Entry fromView(InventoryEntryView entryView) {
            return new Entry(
                    entryView.itemInstanceId(),
                    entryView.slotIndex(),
                    entryView.itemId(),
                    entryView.itemName(),
                    entryView.category().name(),
                    entryView.type().name(),
                    entryView.rarity().name(),
                    entryView.stackable(),
                    entryView.maxStack(),
                    entryView.quantity(),
                    entryView.bound(),
                    entryView.durability(),
                    entryView.instanceAttrs() == null ? Map.of() : entryView.instanceAttrs().attrs()
            );
        }
    }

    public record Entries(List<Entry> entryViews) {
        public static Entries fromViews(List<InventoryEntryView> entryViews) {
            return new Entries(
                    entryViews.stream()
                            .map(Entry::fromView)
                            .toList()
            );
        }
    }
}
