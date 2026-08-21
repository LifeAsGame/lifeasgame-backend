package online.lifeasgame.notification.domain.repository;

import java.time.Instant;
import java.util.Optional;
import online.lifeasgame.notification.domain.PlayerNotification;

public interface PlayerNotificationRepository {

    PlayerNotification saveAndFlush(PlayerNotification notification);

    boolean existsByPlayerIdAndSourceEventId(Long playerId, String sourceEventId);

    Optional<PlayerNotification> findOwned(Long notificationId, Long playerId);

    int markAllUnread(Long playerId, Instant readAt);
}
