package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.PlayerEquipment;

import java.util.List;
import java.util.Optional;

public interface PlayerEquipmentRepository {
    Optional<PlayerEquipment> findByPlayerIdAndSlotIdForUpdate(Long playerId, Long slotId);

    boolean existsByItemInstanceId(Long itemInstanceId);

    List<PlayerEquipment> findByPlayerId(Long playerId);
}
