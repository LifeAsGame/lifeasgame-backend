package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.command.PlayerEquipmentCommand;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.presentation.request.PlayerEquipmentRequest;
import online.lifeasgame.character.presentation.response.PlayerEquipmentResponse;

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
}
