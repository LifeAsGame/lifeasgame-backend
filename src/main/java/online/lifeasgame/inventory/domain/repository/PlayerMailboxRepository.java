package online.lifeasgame.inventory.domain.repository;

import java.util.Optional;
import online.lifeasgame.inventory.domain.PlayerMailbox;

public interface PlayerMailboxRepository {
    Optional<PlayerMailbox> findByPlayerId(Long playerId);
    Optional<PlayerMailbox> findByPlayerIdForUpdate(Long playerId);
    PlayerMailbox save(PlayerMailbox box);
    void insertIfAbsent(Long playerId, int capacitySlots);
}
