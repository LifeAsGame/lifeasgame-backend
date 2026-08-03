package online.lifeasgame.inventory.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.query.InventoryStackQuery;
import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.repository.PlayerInventoryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerInventoryAdapter implements PlayerInventoryRepository, InventoryStackQuery {

    private final JpaInventoryRepository jpaRepository;

    @Override
    public Optional<PlayerInventory> findByPlayerId(Long playerId) {
        return jpaRepository.findByPlayerId(playerId);
    }

    @Override
    public PlayerInventory save(PlayerInventory inv) {
        return jpaRepository.save(inv);
    }

    @Override
    public void insertIfAbsent(Long playerId, int capacitySlots) {
        jpaRepository.insertIfAbsent(playerId, capacitySlots);
    }

    @Override
    public long countStacksExceeding(Long itemId, int limit) {
        return jpaRepository.countStacksExceeding(itemId, limit);
    }
}
