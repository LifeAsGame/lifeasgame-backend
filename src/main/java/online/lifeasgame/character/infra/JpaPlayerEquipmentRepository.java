package online.lifeasgame.character.infra;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import online.lifeasgame.character.domain.PlayerEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;

public interface JpaPlayerEquipmentRepository extends JpaRepository<PlayerEquipment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    @Query("SELECT pe FROM PlayerEquipment pe WHERE pe.playerId = :playerId AND pe.slotId = :slotId")
    Optional<PlayerEquipment> findByPlayerIdAndSlotIdForUpdate(Long playerId, Long slotId);

    boolean existsByPlayerIdAndItemInstanceId(
            Long playerId,
            Long instanceId
    );

    List<PlayerEquipment> findByPlayerId(Long playerId);
}
