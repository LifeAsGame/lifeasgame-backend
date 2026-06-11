package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.request.PlayerEquipmentRequest;
import online.lifeasgame.character.api.player.response.PlayerEquipmentResponse;
import online.lifeasgame.character.application.command.PlayerEquipmentCommand;
import online.lifeasgame.character.application.result.PlayerEquipmentResult;

import java.util.List;

public final class PlayerEquipmentWebMapper {

    private PlayerEquipmentWebMapper() {}

    public static PlayerEquipmentCommand.Equip toEquipCommand(Long slotId, PlayerEquipmentRequest.Equip request) {
        return new PlayerEquipmentCommand.Equip(slotId, request.itemInstanceId());
    }

    public static PlayerEquipmentResponse.Equipped toEquipped(PlayerEquipmentResult.Equipped result) {
        return new PlayerEquipmentResponse.Equipped(result.slotId(), result.itemInstanceId());
    }

    public static PlayerEquipmentResponse.Infos toInfos(List<PlayerEquipmentResult.Info> results) {
        return new PlayerEquipmentResponse.Infos(
                results.stream()
                        .map(
                                result ->
                                        new PlayerEquipmentResponse.Info(
                                                result.slotId(),
                                                result.slotCode(),
                                                result.slotName(),
                                                result.slotCategory(),
                                                result.slotRole(),
                                                result.itemInstanceId()
                                        )
                        )
                        .toList()
        );
    }

    public static PlayerEquipmentResponse.UnEquipped toUnEquipped(Long slotId) {
        return new PlayerEquipmentResponse.UnEquipped(slotId);
    }
}
