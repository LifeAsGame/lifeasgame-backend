package online.lifeasgame.inventory.domain.repository;

import java.util.Optional;
import online.lifeasgame.inventory.domain.PlayerMailbox;

public interface PlayerMailboxRepository {
    Optional<PlayerMailbox> findByPlayerId(Long playerId);
    PlayerMailbox save(PlayerMailbox box);
}
