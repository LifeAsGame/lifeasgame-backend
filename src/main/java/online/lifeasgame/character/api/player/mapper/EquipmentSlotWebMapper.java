package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.EquipmentSlotResult;
import online.lifeasgame.character.api.player.response.EquipmentSlotResponse;

public class EquipmentSlotWebMapper {

    private EquipmentSlotWebMapper() {}

    public static EquipmentSlotResponse.Infos toEquipmentSlotInfos(List<EquipmentSlotResult.Info> infos) {
        return EquipmentSlotResponse.Infos.of(
                infos.stream()
                        .map(
                                info ->
                                        EquipmentSlotResponse.Info.of(
                                                info.code(),
                                                info.name(),
                                                info.category(),
                                                info.role()
                                        )
                        )
                        .toList()
        );
    }
}
