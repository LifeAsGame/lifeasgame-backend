package online.lifeasgame.character.infra;

import java.util.Collection;
import java.util.List;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.EquipmentSlotRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEquipmentSlotRepository extends JpaRepository<EquipmentSlot, Long> {
    List<EquipmentSlot> findByCategoryIn(Collection<EquipmentSlotCategory> categories);

    List<EquipmentSlot> findByRoleIn(Collection<EquipmentSlotRole> roles);

    List<EquipmentSlot> findByCategoryInAndRoleIn(Collection<EquipmentSlotCategory> categories, Collection<EquipmentSlotRole> roles);
}
