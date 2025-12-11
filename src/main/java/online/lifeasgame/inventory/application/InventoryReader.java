package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.inventory.domain.repository.PlayerInventoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class InventoryReader {

    private final PlayerInventoryRepository repository;

    public PlayerInventory getByPlayerIdOrThrow(Long playerId) {
        return repository.findByPlayerId(playerId)
                .orElseThrow(() -> new DomainException(InventoryError.CONTAINER_NOT_FOUND));
    }
}
