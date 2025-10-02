package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.inventory.domain.repository.PlayerMailboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class MailboxReader {
    private final PlayerMailboxRepository mailboxRepository;

    public PlayerMailbox getPlayerMailbox(Long playerId) {
        return mailboxRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new DomainException(InventoryError.CONTAINER_NOT_FOUND));
    }
}
