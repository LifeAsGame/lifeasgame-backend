package online.lifeasgame.character.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.PlayerEquipment;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PlayerEquipmentReader {

    private final PlayerEquipmentRepository playerEquipmentRepository;

    public boolean existsItemInstance(Long itemInstanceId) {
        return playerEquipmentRepository.existsByItemInstanceId(itemInstanceId);
    }

    public List<PlayerEquipment> getPlayerEquipmentInfos(Long playerId) {
        return playerEquipmentRepository.findByPlayerId(playerId);
    }
}
