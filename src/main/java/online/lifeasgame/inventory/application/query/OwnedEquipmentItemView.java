package online.lifeasgame.inventory.application.query;

import online.lifeasgame.inventory.domain.EquipmentCompatibilityKind;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;

public record OwnedEquipmentItemView(
        Long itemInstanceId,
        Long itemId,
        ItemCategory category,
        ItemType type,
        EquipmentCompatibilityKind equipmentCompatibilityKind
) {
}
