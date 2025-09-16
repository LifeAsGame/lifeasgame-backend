package online.lifeasgame.character.domain.repository;

import java.util.Optional;
import online.lifeasgame.character.domain.PlayerEquipment;

public interface PlayerEquipmentRepository {
    Optional<PlayerEquipment> findByPlayerIdAndSlotId(Long playerId, Long slotId);

    Optional<PlayerEquipment> findByPlayerIdAndSlotIdForUpdate(Long playerId, Long slotId);    // write

    boolean existsByItemInstanceId(Long itemInstanceId);
}
