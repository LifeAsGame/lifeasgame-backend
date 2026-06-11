package online.lifeasgame.character.domain;

import java.util.List;
import online.lifeasgame.character.domain.error.EquipmentSlotError;
import online.lifeasgame.core.lang.EnumParsers;

public enum EquipmentSlotRole {
    SINGLE,   // 대부분의 단일 슬롯
    MAIN,     // 주무기
    OFFHAND,  // 보조무기/방패
    LEFT,     // 반지(좌)
    RIGHT     // 반지(우)
    ;

    public static EquipmentSlotRole parse(String raw) {
        return EnumParsers.parseStrict(
                EquipmentSlotRole.class,
                raw,
                EquipmentSlotError.INVALID_EQUIPMENT_SLOT_ROLE,
                "EquipmentSlot Role"
        );
    }

    public static List<EquipmentSlotRole> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                EquipmentSlotRole.class,
                raw,
                EquipmentSlotError.INVALID_EQUIPMENT_SLOT_ROLE,
                "EquipmentSlot Role"
        );
    }
}
