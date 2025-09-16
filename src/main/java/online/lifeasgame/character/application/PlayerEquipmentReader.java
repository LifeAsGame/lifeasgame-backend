package online.lifeasgame.character.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.domain.repository.PlayerEquipmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PlayerEquipmentReader {

    private final PlayerEquipmentRepository playerEquipmentRepository;

    public boolean existsItemInstance(Long itemInstanceId) {
        return playerEquipmentRepository.existsByItemInstanceId(itemInstanceId);
    }
}
