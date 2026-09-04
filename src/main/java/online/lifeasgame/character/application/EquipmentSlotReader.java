package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.EquipmentSlot;
import online.lifeasgame.character.domain.EquipmentSlotCategory;
import online.lifeasgame.character.domain.EquipmentSlotRole;
import online.lifeasgame.character.domain.error.EquipmentSlotError;
import online.lifeasgame.character.domain.repository.EquipmentSlotRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class EquipmentSlotReader {

    private final EquipmentSlotRepository repository;

    public List<EquipmentSlot> getByCategoriesAndRoles(List<EquipmentSlotCategory> categories, List<EquipmentSlotRole> roles) {
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

    public EquipmentSlot getByIdOrThrow(Long slotId) {
        return repository.findById(slotId)
                .orElseThrow(() -> new DomainException(EquipmentSlotError.EQUIPMENT_SLOT_NOT_FOUND));
    }

    public List<EquipmentSlot> getAll() {
        return repository.findAll();
    }
}
