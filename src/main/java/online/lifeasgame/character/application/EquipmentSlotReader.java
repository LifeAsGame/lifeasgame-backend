package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.EquipmentSlotRole;
import online.lifeasgame.character.domain.repository.EquipmentSlotRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class EquipmentSlotReader {

    private final EquipmentSlotRepository repository;

    public List<EquipmentSlot> getEquipmentSlots(List<EquipmentSlotCategory> categories, List<EquipmentSlotRole> roles) {
        boolean catsEmpty = (categories == null || categories.isEmpty());
        boolean rolesEmpty = (roles == null || roles.isEmpty());

        if (catsEmpty && rolesEmpty) {
            return repository.findAll();
        } else if (catsEmpty) {
            return repository.findByRoleIn(roles);
        } else if (rolesEmpty) {
            return repository.findByCategoryIn(categories);
        } else {
            return repository.findByCategoryInAndRoleIn(categories, roles);
        }
    }
}
