package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PlayerEquipmentWriter {

    private static final String EQUIPPED_ITEM_UNIQUE =
            "uq_player_equipment_item";

    private final PlayerEquipmentRepository repository;

    public List<PlayerEquipment> createEmpty(
            Long playerId,
            List<Long> slotIds
    ) {
        return repository.saveAllAndFlush(slotIds.stream()
                .map(slotId -> PlayerEquipment.create(
                        playerId,
                        slotId,
                        null
                ))
                .toList());
    }

    public EquipmentReplacement equip(Long playerId, Long slotId, Long itemInstanceId) {
        PlayerEquipment playerEquipment = getByPlayerIdAndSlotIdForUpdate(playerId, slotId);
        Long previousItemInstanceId = playerEquipment.getItemInstanceId();
        playerEquipment.equip(itemInstanceId);
        try {
            return new EquipmentReplacement(
                    repository.saveAndFlush(playerEquipment),
                    previousItemInstanceId
            );
        } catch (DataIntegrityViolationException exception) {
            if (isEquippedItemConflict(exception)) {
                throw new DomainException(
                        PlayerEquipmentError.ALREADY_EQUIPPED_ITEM,
                        null,
                        exception
                );
            }
            throw exception;
        }
    }

    public Long unEquip(Long playerId, Long slotId) {
        PlayerEquipment playerEquipment = getByPlayerIdAndSlotIdForUpdate(playerId, slotId);
        Long previousItemInstanceId = playerEquipment.getItemInstanceId();
        playerEquipment.unEquip();
        return previousItemInstanceId;
    }

    record EquipmentReplacement(
            PlayerEquipment equipment,
            Long previousItemInstanceId
    ) {
    }

    private PlayerEquipment getByPlayerIdAndSlotIdForUpdate(Long playerId, Long slotId) {
        return repository.findByPlayerIdAndSlotIdForUpdate(playerId, slotId)
                .orElseThrow(() -> new DomainException(PlayerEquipmentError.PLAYER_EQUIPMENT_NOT_FOUND));
    }

    private boolean isEquippedItemConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null;
             cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && EQUIPPED_ITEM_UNIQUE.equalsIgnoreCase(
                    violation.getConstraintName()
            )) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null && message.toLowerCase()
                    .contains(EQUIPPED_ITEM_UNIQUE)) {
                return true;
            }
        }
        return false;
    }
}
