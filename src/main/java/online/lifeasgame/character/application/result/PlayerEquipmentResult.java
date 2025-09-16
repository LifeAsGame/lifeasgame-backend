package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.PlayerEquipment;

public class PlayerEquipmentResult {

    private PlayerEquipmentResult() {
    }

    public record EquippedEquipment(Long slotId, Long itemInstanceId) {
        public static EquippedEquipment of(PlayerEquipment playerEquipment) {
            return new EquippedEquipment(
                    playerEquipment.getSlotId(),
                    playerEquipment.getItemInstanceId()
            );
        }
    }
}
