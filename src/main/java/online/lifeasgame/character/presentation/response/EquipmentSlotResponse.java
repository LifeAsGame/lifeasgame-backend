package online.lifeasgame.character.presentation.response;

import java.util.List;

public class EquipmentSlotResponse {

    private EquipmentSlotResponse() {
    }

    public record EquipmentSlotInfos(
            List<EquipmentSlotResponse.EquipmentSlotInfo> EquipmentSlotInfos
    ) {
        public static EquipmentSlotResponse.EquipmentSlotInfos of(List<EquipmentSlotResponse.EquipmentSlotInfo> EquipmentSlotInfos) {
            return new EquipmentSlotResponse.EquipmentSlotInfos(EquipmentSlotInfos);
        }
    }

    public record EquipmentSlotInfo(
            String code,
            String name,
            String category,
            String role
    ) {
        public static EquipmentSlotResponse.EquipmentSlotInfo of(String code, String name, String category, String role) {
            return new EquipmentSlotResponse.EquipmentSlotInfo(code, name, category, role);
        }
    }
}
