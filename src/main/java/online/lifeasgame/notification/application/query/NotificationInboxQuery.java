package online.lifeasgame.notification.application.query;

import java.time.Instant;
import java.util.List;
import online.lifeasgame.notification.domain.NotificationType;

public interface NotificationInboxQuery {

    List<Row> findInbox(Long playerId, Long cursor, int limit);

    long countUnread(Long playerId);

    record Row(
            Long id,
            NotificationType type,
            String title,
            String body,
            Instant occurredAt,
            Instant readAt
    ) {
    }
}
