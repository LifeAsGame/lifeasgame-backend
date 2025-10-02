package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.api.player.request.PlayerEquipmentRequest;
import online.lifeasgame.character.api.player.response.PlayerEquipmentResponse;

public class PlayerEquipmentWebMapper {

    private PlayerEquipmentWebMapper() {}

    public static PlayerEquipmentCommand.EquipEquipment toCommand(Long slotId, PlayerEquipmentRequest.EquipEquipment request) {
        return PlayerEquipmentCommand.EquipEquipment.of(
                slotId,
                request.itemInstanceId()
        );
    }

    public static PlayerEquipmentResponse.EquippedEquipment toEquippedEquipment(
            PlayerEquipmentResult.EquippedEquipment equippedEquipment
    ) {
        return PlayerEquipmentResponse.EquippedEquipment.of(equippedEquipment);
    }

    public static PlayerEquipmentResponse.PlayerEquipmentInfos toPlayerEquipmentInfos(List<PlayerEquipmentResult.PlayerEquipmentInfo> playerEquipmentInfos) {
        return PlayerEquipmentResponse.PlayerEquipmentInfos.of(
                playerEquipmentInfos.stream()
                        .map(
                                playerEquipmentInfo ->
                                        PlayerEquipmentResponse.PlayerEquipmentInfo.of(
                                                playerEquipmentInfo.slotId(),
                                                playerEquipmentInfo.itemInstanceId()
                                        )
                        )
                        .toList()
        );
    }
}
