package online.lifeasgame.character.infra;


import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.EquipmentSlotRole;
import online.lifeasgame.character.domain.repository.EquipmentSlotRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EquipmentSlotRepositoryAdapter implements EquipmentSlotRepository {

    private final JpaEquipmentSlotRepository jpaRepository;

    @Override
    public List<EquipmentSlot> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<EquipmentSlot> findByCategoryIn(List<EquipmentSlotCategory> categories) {
        return jpaRepository.findByCategoryIn(categories);
    }

    @Override
    public List<EquipmentSlot> findByRoleIn(List<EquipmentSlotRole> roles) {
        return jpaRepository.findByRoleIn(roles);
    }

    @Override
    public List<EquipmentSlot> findByCategoryInAndRoleIn(List<EquipmentSlotCategory> categories, List<EquipmentSlotRole> roles) {
        return jpaRepository.findByCategoryInAndRoleIn(categories, roles);
    }
}
