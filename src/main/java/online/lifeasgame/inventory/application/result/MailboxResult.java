package online.lifeasgame.inventory.application.result;

import online.lifeasgame.inventory.application.query.MailboxEntryView;

import java.util.List;
import java.util.Map;

public final class MailboxResult {

    private MailboxResult() {}

    public record Slot(int slot) {
    }

    public record Entry(
            Long mailId,
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
        public static Entry fromView(MailboxEntryView entryView) {
            return new Entry(
                    entryView.mailId(),
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

    public record Entries(List<Entry> entries) {
        public static Entries fromViews(List<MailboxEntryView> entryViews) {
            return new Entries(
                    entryViews.stream()
                            .map(Entry::fromView)
                            .toList()
            );
        }
    }
}
