package online.lifeasgame.character.infra;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import online.lifeasgame.character.domain.PlayerEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface JpaPlayerEquipmentRepository extends JpaRepository<PlayerEquipment, Long> {
    Optional<PlayerEquipment> findByPlayerIdAndSlotId(Long playerId, Long slotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    @Query("SELECT pe FROM PlayerEquipment pe WHERE pe.playerId = :playerId AND pe.slotId = :slotId")
    Optional<PlayerEquipment> findByPlayerIdAndSlotIdForUpdate(Long playerId, Long slotId);

    boolean existsByItemInstanceId(Long itemInstanceId);
}
