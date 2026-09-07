package online.lifeasgame.character.domain.repository;

import online.lifeasgame.character.domain.PlayerEquipment;

import java.util.List;
import java.util.Optional;

public interface PlayerEquipmentRepository {
    Optional<PlayerEquipment> findByPlayerIdAndSlotIdForUpdate(Long playerId, Long slotId);

    List<PlayerEquipment> findByPlayerId(Long playerId);

    List<PlayerEquipment> findByPlayerIdForUpdate(Long playerId);

    boolean existsByPlayerIdAndItemInstanceId(
            Long playerId,
            Long instanceId
    );

    PlayerEquipment save(PlayerEquipment playerEquipment);

    PlayerEquipment saveAndFlush(PlayerEquipment playerEquipment);

    List<PlayerEquipment> saveAllAndFlush(
            List<PlayerEquipment> playerEquipment
    );
}
