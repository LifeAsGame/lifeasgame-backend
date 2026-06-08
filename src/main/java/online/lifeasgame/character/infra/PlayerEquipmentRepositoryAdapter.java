package online.lifeasgame.character.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerEquipmentRepositoryAdapter implements PlayerEquipmentRepository {

    private final JpaPlayerEquipmentRepository jpaRepository;

    @Override
    public Optional<PlayerEquipment> findByPlayerIdAndSlotIdForUpdate(Long playerId, Long slotId) {
        return jpaRepository.findByPlayerIdAndSlotIdForUpdate(playerId, slotId);
    }

    @Override
    public boolean existsByPlayerIdAndSlotIdAndItemInstanceId(Long playerId, Long slotId, Long instanceId) {
        return jpaRepository.existsByPlayerIdAndSlotIdAndItemInstanceId(playerId, slotId, instanceId);
    }

    @Override
    public PlayerEquipment save(PlayerEquipment playerEquipment) {
        return jpaRepository.save(playerEquipment);
    }

    @Override
    public List<PlayerEquipment> findByPlayerId(Long playerId) {
        return jpaRepository.findByPlayerId(playerId);
    }
}
