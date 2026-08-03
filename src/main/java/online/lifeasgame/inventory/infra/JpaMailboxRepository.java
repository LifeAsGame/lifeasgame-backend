package online.lifeasgame.inventory.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaMailboxRepository extends JpaRepository<PlayerMailbox, Long> {
    Optional<PlayerMailbox> findByPlayerId(Long playerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mailbox FROM PlayerMailbox mailbox WHERE mailbox.playerId = :playerId")
    Optional<PlayerMailbox> findByPlayerIdForUpdate(@Param("playerId") Long playerId);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO player_mailbox (
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
                FROM MailboxEntry e
                WHERE e.itemId = :itemId
                AND e.quantity.value > :limit
            """
    )
    long countStacksExceeding(@Param("itemId") Long itemId, @Param("limit") int limit);
}
