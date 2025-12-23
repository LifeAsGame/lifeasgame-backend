package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerEquipmentWriter {

    private final PlayerEquipmentRepository repository;

    public PlayerEquipment equip(Long playerId, Long slotId, Long itemInstanceId) {
        PlayerEquipment playerEquipment = getByPlayerIdAndSlotIdForUpdate(playerId, slotId);
        playerEquipment.equip(itemInstanceId);
        return playerEquipment;
    }

    public void unEquip(Long playerId, Long slotId) {
        PlayerEquipment playerEquipment = getByPlayerIdAndSlotIdForUpdate(playerId, slotId);
        playerEquipment.unEquip();
    }

    private PlayerEquipment getByPlayerIdAndSlotIdForUpdate(Long playerId, Long slotId) {
        return repository.findByPlayerIdAndSlotIdForUpdate(playerId, slotId)
                .orElseThrow(() -> new DomainException(PlayerEquipmentError.PLAYER_EQUIPMENT_NOT_FOUND));
    }
}
