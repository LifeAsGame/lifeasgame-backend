package online.lifeasgame.inventory.domain.repository;

import java.util.Optional;
import online.lifeasgame.inventory.domain.PlayerInventory;

public interface PlayerInventoryRepository {
    Optional<PlayerInventory> findByPlayerId(Long playerId);
    PlayerInventory save(PlayerInventory inv);
}
