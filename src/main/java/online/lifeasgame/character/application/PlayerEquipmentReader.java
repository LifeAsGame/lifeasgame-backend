package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerEquipmentReader {

    private final PlayerEquipmentRepository repository;

    public List<PlayerEquipment> getByPlayerId(Long playerId) {
        return repository.findByPlayerId(playerId);
    }

    public void assertNotEquipped(Long playerId, Long instanceId) {
        if (repository.existsByPlayerIdAndItemInstanceId(
                playerId,
                instanceId
        )) {
            throw new DomainException(PlayerEquipmentError.ALREADY_EQUIPPED_ITEM);
        }
    }
}
