package online.lifeasgame.character.api.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.EquipmentSlotResult;
import online.lifeasgame.character.api.response.EquipmentSlotResponse;

public class EquipmentSlotWebMapper {

    private EquipmentSlotWebMapper() {}

    public static EquipmentSlotResponse.EquipmentSlotInfos toEquipmentSlotInfos(List<EquipmentSlotResult.EquipmentSlotInfo> equipmentSlotInfos) {
        return EquipmentSlotResponse.EquipmentSlotInfos.of(
                equipmentSlotInfos.stream()
                        .map(
                                equipmentSlotInfo ->
                                        EquipmentSlotResponse.EquipmentSlotInfo.of(
                                                equipmentSlotInfo.code(),
                                                equipmentSlotInfo.name(),
                                                equipmentSlotInfo.category(),
                                                equipmentSlotInfo.role()
                                        )
                        )
                        .toList()
        );
    }
}
