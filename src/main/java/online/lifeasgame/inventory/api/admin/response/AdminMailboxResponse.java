package online.lifeasgame.inventory.api.admin.response;

import java.util.List;
import java.util.Map;

public final class AdminMailboxResponse {

    private AdminMailboxResponse() {}

    public record Slot(int slot) {
    }

    public record Meta(
            int capacitySlots,
            int usedSlots,
            int freeSlots
    ) {}

    public record Mail(
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

    public record View(Meta meta, List<Mail> mails) {}
}
