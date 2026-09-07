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
    public boolean existsByPlayerIdAndItemInstanceId(
            Long playerId,
            Long instanceId
    ) {
        return jpaRepository.existsByPlayerIdAndItemInstanceId(
                playerId,
                instanceId
        );
    }

    @Override
    public PlayerEquipment save(PlayerEquipment playerEquipment) {
        return jpaRepository.save(playerEquipment);
    }

    @Override
    public PlayerEquipment saveAndFlush(PlayerEquipment playerEquipment) {
        return jpaRepository.saveAndFlush(playerEquipment);
    }

    @Override
    public List<PlayerEquipment> saveAllAndFlush(
            List<PlayerEquipment> playerEquipment
    ) {
        return jpaRepository.saveAllAndFlush(playerEquipment);
    }

    @Override
    public List<PlayerEquipment> findByPlayerId(Long playerId) {
        return jpaRepository.findByPlayerId(playerId);
    }

    @Override
    public List<PlayerEquipment> findByPlayerIdForUpdate(Long playerId) {
        return jpaRepository.findByPlayerIdForUpdate(playerId);
    }
}
