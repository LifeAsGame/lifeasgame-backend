package online.lifeasgame.character.application;

import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;

final class EquipmentCompatibilityPolicy {

    private EquipmentCompatibilityPolicy() {
    }

    static void validate(
            EquipmentSlotCategory slot,
            String itemCategory,
            String itemType,
            String equipmentCompatibilityKind
    ) {
        if (equipmentCompatibilityKind != null) {
            if (!slot.name().equals(equipmentCompatibilityKind)) {
                throw new DomainException(
                        PlayerEquipmentError.ITEM_NOT_COMPATIBLE_WITH_SLOT
                );
            }
            return;
        }

        boolean compatible = switch (slot) {
            case WEAPON -> itemCategory.equals("WEAPON");
            case HEAD -> itemCategory.equals("ARMOR")
                    && itemType.equals("HELMET");
            case CHEST -> itemCategory.equals("ARMOR")
                    && itemType.equals("CHEST");
            case RING -> itemCategory.equals("ACCESSORY")
                    && itemType.equals("RING");
            case LEGS, HANDS, FEET, NECK, TRINKET -> throw new DomainException(
                    PlayerEquipmentError.UNSUPPORTED_EQUIPMENT_SLOT
            );
        };
        if (!compatible) {
            throw new DomainException(
                    PlayerEquipmentError.ITEM_NOT_COMPATIBLE_WITH_SLOT
            );
        }
    }
}
