package online.lifeasgame.inventory.infra;

import java.util.Optional;
import online.lifeasgame.inventory.domain.PlayerInventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInventoryRepository extends JpaRepository<PlayerInventory, Long> {
    Optional<PlayerInventory> findByPlayerId(Long playerId);
}
