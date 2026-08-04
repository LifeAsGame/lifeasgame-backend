package online.lifeasgame.inventory.domain.repository;

import java.util.Optional;
import online.lifeasgame.inventory.domain.PlayerInventory;

public interface PlayerInventoryRepository {
    Optional<PlayerInventory> findByPlayerId(Long playerId);
    Optional<PlayerInventory> findByPlayerIdForUpdate(Long playerId);
    PlayerInventory save(PlayerInventory inv);
    void insertIfAbsent(Long playerId, int capacitySlots);
}
