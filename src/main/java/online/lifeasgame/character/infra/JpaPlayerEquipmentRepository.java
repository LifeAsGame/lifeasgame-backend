package online.lifeasgame.character.infra;

import java.util.Optional;
import online.lifeasgame.character.domain.PlayerEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlayerEquipmentRepository extends JpaRepository<PlayerEquipment, Long> {
    Optional<PlayerEquipment> findByPlayerIdAndSlotId(Long playerId, Long slotId);

    boolean existsByItemInstanceId(Long itemInstanceId);
}
