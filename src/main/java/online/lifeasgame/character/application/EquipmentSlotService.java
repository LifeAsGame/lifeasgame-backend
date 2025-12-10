package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.EquipmentSlotResult;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.EquipmentSlotRole;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentSlotService {

    private final EquipmentSlotReader equipmentSlotReader;

    public List<EquipmentSlotResult.Info> getEquipmentSlots(List<String> categories, List<String> roles) {
        List<EquipmentSlot> equipmentSlots = equipmentSlotReader.getByCategoriesAndRoles(
                EquipmentSlotCategory.parse(categories),
                EquipmentSlotRole.parse(roles)
        );

        return EquipmentSlotResult.Info.fromList(equipmentSlots);
    }
}
