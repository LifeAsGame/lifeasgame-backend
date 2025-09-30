package online.lifeasgame.inventory.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.repository.PlayerInventoryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerInventoryRepositoryAdapter implements PlayerInventoryRepository {

    private final JpaInventoryRepository jpa;

    @Override
    public Optional<PlayerInventory> findByPlayerId(Long playerId) {
        return jpa.findByPlayerId(playerId);
    }

    @Override
    public PlayerInventory save(PlayerInventory inv) {
        return jpa.save(inv);
    }
}
