package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.inventory.domain.repository.PlayerInventoryRepository;
import online.lifeasgame.inventory.domain.repository.PlayerMailboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryContainerProvisioningService {

    private final PlayerInventoryRepository inventoryRepository;
    private final PlayerMailboxRepository mailboxRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void ensureContainers(Long playerId) {
        if (playerId == null || playerId <= 0) {
            throw new DomainException(InventoryError.PLAYER_ID_INVALID);
        }

        mailboxRepository.insertIfAbsent(
                playerId,
                PlayerMailbox.DEFAULT_CAPACITY
        );
        inventoryRepository.insertIfAbsent(
                playerId,
                PlayerInventory.DEFAULT_CAPACITY
        );
    }
}
