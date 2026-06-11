package online.lifeasgame.inventory.api.player.response;


import java.util.List;
import java.util.Map;

public final class MailboxResponse {

    private MailboxResponse() {
    }

    public record Slot(int slot) {
    }

    public record Meta(
            int capacitySlots,
            int usedSlots,
            int freeSlots
    ) {}

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
    ) {}

    public record Entries(List<Entry> entries) {
    }

    public record View(
            Meta meta,
            List<Entry> entries
    ) {}

    public record Claimed(
            List<Integer> claimedSlotIndexes
    ) {}
}
