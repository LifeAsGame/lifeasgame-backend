package online.lifeasgame.character.presentation.response;

import online.lifeasgame.character.application.result.PlayerEquipmentResult;

public class PlayerEquipmentResponse {

    private PlayerEquipmentResponse() {
    }

    public record EquippedEquipment(Long slotId, Long itemInstanceId) {
        public static EquippedEquipment of(PlayerEquipmentResult.EquippedEquipment equippedEquipment) {
            return new EquippedEquipment(
                    equippedEquipment.slotId(),
                    equippedEquipment.itemInstanceId()
            );
        }
    }
}
