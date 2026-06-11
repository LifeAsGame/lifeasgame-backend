package online.lifeasgame.character.domain;

import java.util.List;
import online.lifeasgame.character.domain.error.EquipmentSlotError;
import online.lifeasgame.core.lang.EnumParsers;

public enum EquipmentSlotCategory {
    WEAPON,        // WEAPON_MAIN / OFFHAND 묶음 정책 (양손/보조무기 규칙 등)
    HEAD, CHEST, LEGS, HANDS, FEET,
    NECK, RING, TRINKET
    ;

    public static EquipmentSlotCategory parse(String raw) {
        return EnumParsers.parseStrict(
                EquipmentSlotCategory.class,
                raw,
                EquipmentSlotError.INVALID_EQUIPMENT_SLOT_CATEGORY,
                "Equipment Slot Category"
        );
    }

    public static List<EquipmentSlotCategory> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                EquipmentSlotCategory.class,
                raw,
                EquipmentSlotError.INVALID_EQUIPMENT_SLOT_CATEGORY,
                "Equipment Slot Category"
        );
    }
}
