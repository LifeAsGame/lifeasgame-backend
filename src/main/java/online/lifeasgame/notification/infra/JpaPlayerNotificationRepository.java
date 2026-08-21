package online.lifeasgame.notification.infra;

import java.time.Instant;
import java.util.Optional;
import online.lifeasgame.notification.domain.PlayerNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPlayerNotificationRepository
        extends JpaRepository<PlayerNotification, Long> {

    boolean existsByPlayerIdAndSourceEventId(Long playerId, String sourceEventId);

    Optional<PlayerNotification> findByIdAndPlayerId(Long id, Long playerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PlayerNotification notification
            set notification.readAt = :readAt,
                notification.updatedAt = :readAt
            where notification.playerId = :playerId
              and notification.readAt is null
            """)
    int markAllUnread(
            @Param("playerId") Long playerId,
            @Param("readAt") Instant readAt
    );
}
