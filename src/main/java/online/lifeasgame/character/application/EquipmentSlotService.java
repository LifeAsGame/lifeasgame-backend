package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.EquipmentSlotResult;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.EquipmentSlotRole;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EquipmentSlotService {

    private final EquipmentSlotReader equipmentSlotReader;

    public List<EquipmentSlotResult.Info> getEquipmentSlots(List<String> categories, List<String> roles) {
        List<EquipmentSlot> EquipmentSlots = equipmentSlotReader.getEquipmentSlots(
                EquipmentSlotCategory.parse(categories),
                EquipmentSlotRole.parse(roles)
        );

        return EquipmentSlotResult.Info.fromList(EquipmentSlots);
    }
}
