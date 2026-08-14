package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.inventory.domain.error.ItemError;

public enum EquipmentCompatibilityKind {
    WEAPON(ItemCategory.WEAPON),
    HEAD(ItemCategory.ARMOR),
    CHEST(ItemCategory.ARMOR),
    LEGS(ItemCategory.ARMOR),
    HANDS(ItemCategory.ARMOR),
    FEET(ItemCategory.ARMOR),
    NECK(ItemCategory.ACCESSORY),
    RING(ItemCategory.ACCESSORY),
    TRINKET(ItemCategory.ACCESSORY);

    private final ItemCategory itemCategory;

    EquipmentCompatibilityKind(ItemCategory itemCategory) {
        this.itemCategory = itemCategory;
    }

    public static EquipmentCompatibilityKind parseNullable(String raw) {
        return raw == null ? null : EnumParsers.parseStrict(
                EquipmentCompatibilityKind.class,
                raw,
                ItemError.INVALID_EQUIPMENT_COMPATIBILITY_KIND,
                "Equipment compatibility kind"
        );
    }

    public void validateCategory(ItemCategory category) {
        if (itemCategory != category) {
            throw new DomainException(ItemError.INVALID_EQUIPMENT_COMPATIBILITY);
        }
    }
}
