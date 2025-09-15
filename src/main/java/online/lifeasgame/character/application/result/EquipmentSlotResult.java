package online.lifeasgame.character.application.result;

import java.util.List;
import online.lifeasgame.character.domain.EquipmentSlot;

public class EquipmentSlotResult {

    private EquipmentSlotResult() {
    }

    public record EquipmentSlotInfo(
            String code,
            String name,
            String category,
            String role
    ) {
        public static EquipmentSlotInfo from(EquipmentSlot equipmentSlot) {
            return new EquipmentSlotInfo(
                    equipmentSlot.getCode(),
                    equipmentSlot.getName(),
                    equipmentSlot.getCategory().name(),
                    equipmentSlot.getRole().name()
            );
        }

        public static List<EquipmentSlotInfo> fromList(List<EquipmentSlot> EquipmentSlots) {
            return EquipmentSlots.stream().map(EquipmentSlotResult.EquipmentSlotInfo::from).toList();
        }
    }
}
