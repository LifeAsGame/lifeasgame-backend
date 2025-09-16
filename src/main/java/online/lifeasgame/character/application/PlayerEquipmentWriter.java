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
public class PlayerEquipmentWriter {

    private final PlayerEquipmentRepository playerEquipmentRepository;

    public PlayerEquipment equip(Long playerId, Long slotId, Long itemInstanceId) {
        if (itemInstanceId == null) {
            throw new DomainException(PlayerEquipmentError.INVALID_ITEM_INSTANCE_ID);
        }

        PlayerEquipment playerEquipment = getPlayerEquipmentForUpdate(playerId, slotId);
        playerEquipment.equip(itemInstanceId);
        return playerEquipment;
    }

    public PlayerEquipment unEquip(Long playerId, Long slotId) {
        PlayerEquipment playerEquipment = getPlayerEquipmentForUpdate(playerId, slotId);
        playerEquipment.unEquip();
        return playerEquipment;
    }

    private PlayerEquipment getPlayerEquipment(Long playerId, Long slotId) {
        return playerEquipmentRepository.findByPlayerIdAndSlotId(playerId, slotId)
                .orElseThrow(() -> new DomainException(PlayerEquipmentError.PLAYER_EQUIPMENT_NOT_FOUND));
    }

    private PlayerEquipment getPlayerEquipmentForUpdate(Long playerId, Long slotId) {
        return playerEquipmentRepository.findByPlayerIdAndSlotIdForUpdate(playerId, slotId)
                .orElseThrow(() -> new DomainException(PlayerEquipmentError.PLAYER_EQUIPMENT_NOT_FOUND));
    }
}
