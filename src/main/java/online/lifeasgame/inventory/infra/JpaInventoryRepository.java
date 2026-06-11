package online.lifeasgame.inventory.infra;

import online.lifeasgame.inventory.domain.PlayerInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaInventoryRepository extends JpaRepository<PlayerInventory, Long> {
    Optional<PlayerInventory> findByPlayerId(Long playerId);

    @Query(
            """
                SELECT COUNT(e)
                FROM InventoryEntry e
                WHERE e.itemId = :itemId AND e.quantity.value > :limit
            """
    )
    long countStacksExceeding(@Param("itemId") Long itemId, @Param("limit") int limit);
}
