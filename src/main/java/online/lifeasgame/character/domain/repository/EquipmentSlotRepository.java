package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.EquipmentSlotRole;

import java.util.List;
import java.util.Optional;

public interface EquipmentSlotRepository {

    List<EquipmentSlot> findByCategoryIn(List<EquipmentSlotCategory> categories);

    List<EquipmentSlot> findByRoleIn(List<EquipmentSlotRole> categories);

    List<EquipmentSlot> findByCategoryInAndRoleIn(List<EquipmentSlotCategory> categories, List<EquipmentSlotRole> roles);

    List<EquipmentSlot> findAll();

    Optional<EquipmentSlot> findById(Long slotId);
}
