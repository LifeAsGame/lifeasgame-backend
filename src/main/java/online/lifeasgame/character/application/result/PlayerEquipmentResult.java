package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.EquipmentSlot;
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
            String slotCode,
            String slotName,
            String slotCategory,
            String slotRole,
            Long itemInstanceId
    ) {
        public static Info from(
                PlayerEquipment playerEquipment,
                EquipmentSlot slot
        ) {
            return new Info(
                    playerEquipment.getSlotId(),
                    slot.getCode(),
                    slot.getName(),
                    slot.getCategory() == null
                            ? null
                            : slot.getCategory().name(),
                    slot.getRole() == null
                            ? null
                            : slot.getRole().name(),
                    playerEquipment.getItemInstanceId()
            );
        }
    }
}
