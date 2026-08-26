package online.lifeasgame.inventory.api.admin.response;

import java.util.List;

public final class AdminMailboxResponse {

    private AdminMailboxResponse() {}

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
            Integer durability
    ) {}

    public record Entries(Long playerId, List<Entry> entries) {}

    public record View(Meta meta, List<Entry> entries) {}
}
