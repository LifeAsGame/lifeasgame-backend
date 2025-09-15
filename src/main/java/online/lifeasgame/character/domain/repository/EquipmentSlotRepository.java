package online.lifeasgame.character.domain.repository;

import java.util.List;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.EquipmentSlotRole;

public interface EquipmentSlotRepository {

    List<EquipmentSlot> findByCategoryIn(List<EquipmentSlotCategory> categories);

    List<EquipmentSlot> findByRoleIn(List<EquipmentSlotRole> categories);

    List<EquipmentSlot> findByCategoryInAndRoleIn(List<EquipmentSlotCategory> categories, List<EquipmentSlotRole> roles);

    List<EquipmentSlot> findAll();
}
