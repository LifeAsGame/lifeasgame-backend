package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import online.lifeasgame.core.error.DomainException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerEquipmentReader {

    private final PlayerEquipmentRepository repository;

    public List<PlayerEquipment> getByPlayerId(Long playerId) {
        return repository.findByPlayerId(playerId);
    }

    public void assertNotExistsByItemInstanceId(Long itemInstanceId) {
        if (repository.existsByItemInstanceId(itemInstanceId)) {
            throw new DomainException(PlayerEquipmentError.ALREADY_EQUIPPED_ITEM);
        }
    }
}
