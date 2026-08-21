package online.lifeasgame.notification.application.result;

import java.time.Instant;
import java.util.List;
import online.lifeasgame.notification.domain.NotificationType;

public final class NotificationResult {

    private NotificationResult() {
    }

    public record Page(
            List<Info> notifications,
            boolean hasMore,
            Long nextCursor
    ) {
        public Page {
            notifications = List.copyOf(notifications);
        }
    }

    public record Info(
            Long id,
            NotificationType type,
            String title,
            String body,
            Instant occurredAt,
            boolean read
    ) {
    }
}
