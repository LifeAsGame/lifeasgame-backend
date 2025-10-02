package online.lifeasgame.inventory.infra;

import online.lifeasgame.inventory.domain.PlayerMailbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaMailboxRepository extends JpaRepository<PlayerMailbox, Long> {
    Optional<PlayerMailbox> findByPlayerId(Long playerId);

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
