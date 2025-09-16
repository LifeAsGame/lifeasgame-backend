package online.lifeasgame.character.presentation.response;

import java.util.List;
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

    public record PlayerEquipmentInfos(List<PlayerEquipmentResponse.PlayerEquipmentInfo> playerEquipmentInfos) {
        public static PlayerEquipmentResponse.PlayerEquipmentInfos of(List<PlayerEquipmentResponse.PlayerEquipmentInfo> playerEquipmentInfos) {
            return new PlayerEquipmentResponse.PlayerEquipmentInfos(playerEquipmentInfos);
        }
    }

    public record PlayerEquipmentInfo(
            Long slotId,
            Long itemInstanceId
    ) {
        public static PlayerEquipmentResponse.PlayerEquipmentInfo of(
                Long slotId,
                Long itemInstanceId
        ) {
            return new PlayerEquipmentResponse.PlayerEquipmentInfo(slotId, itemInstanceId);
        }
    }
}
