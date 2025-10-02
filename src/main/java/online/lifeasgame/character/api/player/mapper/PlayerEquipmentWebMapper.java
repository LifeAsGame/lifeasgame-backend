package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;
import online.lifeasgame.character.api.player.request.PlayerEquipmentRequest;
import online.lifeasgame.character.api.player.response.PlayerEquipmentResponse;

public class PlayerEquipmentWebMapper {

    private PlayerEquipmentWebMapper() {}

    public static PlayerEquipmentCommand.Equip toCommand(Long slotId, PlayerEquipmentRequest.Equip request) {
        return PlayerEquipmentCommand.Equip.of(
                slotId,
                request.itemInstanceId()
        );
    }

    public static PlayerEquipmentResponse.Equipped toEquippedEquipment(
            PlayerEquipmentResult.Equipped equipped
    ) {
        return PlayerEquipmentResponse.Equipped.of(equipped);
    }

    public static PlayerEquipmentResponse.Infos toPlayerEquipmentInfos(List<PlayerEquipmentResult.Info> infos) {
        return PlayerEquipmentResponse.Infos.of(
                infos.stream()
                        .map(
                                playerEquipmentInfo ->
                                        PlayerEquipmentResponse.Info.of(
                                                playerEquipmentInfo.slotId(),
                                                playerEquipmentInfo.itemInstanceId()
                                        )
                        )
                        .toList()
        );
    }
}
