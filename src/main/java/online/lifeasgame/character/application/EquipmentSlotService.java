package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.result.EquipmentSlotResult;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.EquipmentSlotRole;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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

        return EquipmentSlotResult.Info.fromList(equipmentSlots.stream()
                .filter(EquipmentSlot::supportsEquipmentCommand)
                .sorted(Comparator.comparingInt(EquipmentSlot::getSortOrder))
                .toList());
    }

    public EquipmentSlotResult.Info getEquipmentSlot(Long slotId) {
        EquipmentSlot equipmentSlot = equipmentSlotReader.getByIdOrThrow(slotId);
        if (!equipmentSlot.supportsEquipmentCommand()) {
            throw new DomainException(
                    PlayerEquipmentError.UNSUPPORTED_EQUIPMENT_SLOT
            );
        }
        return EquipmentSlotResult.Info.from(equipmentSlot);
    }
}
