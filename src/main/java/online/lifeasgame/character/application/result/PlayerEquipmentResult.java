package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.PlayerEquipment;

public final class PlayerEquipmentResult {

    private PlayerEquipmentResult() {
    }

    public record Equipped(Long slotId, Long itemInstanceId) {
        public static Equipped from(PlayerEquipment playerEquipment) {
            return new Equipped(
                    playerEquipment.getSlotId(),
                    playerEquipment.getItemInstanceId()
            );
        }
    }

    public record Info(
            Long slotId,
            Long itemInstanceId
    ) {
        public static Info from(PlayerEquipment playerEquipment) {
            return new Info(
                    playerEquipment.getSlotId(),
                    playerEquipment.getItemInstanceId()
            );
        }
    }
}
