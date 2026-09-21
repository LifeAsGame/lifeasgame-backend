package online.lifeasgame.notification.application.query;

import java.time.Instant;
import java.util.List;

public interface NotificationInboxQuery {

    List<Row> findInbox(Long playerId, Long cursor, int limit);

    long countUnread(Long playerId);

    record Row(
            Long id,
            String type,
            String title,
            String body,
            String titleCopyId,
            Integer titleCopyVersion,
            String bodyCopyId,
            Integer bodyCopyVersion,
            String copyLocale,
            Instant occurredAt,
            Instant readAt
    ) {
    }
}
