package online.lifeasgame.inventory.application.query;

import online.lifeasgame.inventory.domain.InstanceAttrs;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;

public record MailboxEntryView(
        Long mailId,
        int slotIndex,
        Long itemId,
        String itemName,
        ItemCategory category,
        ItemType type,
        Rarity rarity,
        boolean stackable,
        int maxStack,
        int quantity,
        boolean bound,
        Integer durability,
        InstanceAttrs instanceAttrs
) {
}
