package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.response.EquipmentSlotResponse;
import online.lifeasgame.character.application.result.EquipmentSlotResult;

import java.util.List;

public final class EquipmentSlotWebMapper {

    private EquipmentSlotWebMapper() {}

    public static EquipmentSlotResponse.Infos toInfos(List<EquipmentSlotResult.Info> results) {
        return new EquipmentSlotResponse.Infos(
                results.stream()
                        .map(
                                result ->
                                        new EquipmentSlotResponse.Info(
                                                result.code(),
                                                result.name(),
                                                result.category(),
                                                result.role()
                                        )
                        )
                        .toList()
        );
    }
}
