package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.EquipmentSlot;

import java.util.List;

public class EquipmentSlotResult {

    private EquipmentSlotResult() {
    }

    public record Info(
            String code,
            String name,
            String category,
            String role
    ) {
        public static Info from(EquipmentSlot equipmentSlot) {
            return new Info(
                    equipmentSlot.getCode(),
                    equipmentSlot.getName(),
                    equipmentSlot.getCategory().name(),
                    equipmentSlot.getRole().name()
            );
        }

        public static List<Info> fromList(List<EquipmentSlot> EquipmentSlots) {
            return EquipmentSlots.stream().map(Info::from).toList();
        }
    }
}
