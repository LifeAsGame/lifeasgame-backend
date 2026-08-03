package online.lifeasgame.inventory.infra;

import online.lifeasgame.inventory.domain.PlayerInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaInventoryRepository extends JpaRepository<PlayerInventory, Long> {
    Optional<PlayerInventory> findByPlayerId(Long playerId);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO player_inventory (
                player_id,
                capacity_slots,
                version,
                created_at,
                updated_at
            ) VALUES (
                :playerId,
                :capacitySlots,
                0,
                CURRENT_TIMESTAMP(6),
                CURRENT_TIMESTAMP(6)
            )
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("playerId") Long playerId,
            @Param("capacitySlots") int capacitySlots
    );

    @Query(
            """
                SELECT COUNT(e)
                FROM InventoryEntry e
                WHERE e.itemId = :itemId AND e.quantity.value > :limit
            """
    )
    long countStacksExceeding(@Param("itemId") Long itemId, @Param("limit") int limit);
}
