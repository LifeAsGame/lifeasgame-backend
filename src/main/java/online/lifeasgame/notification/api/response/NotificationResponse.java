package online.lifeasgame.notification.api.response;

import java.time.Instant;
import java.util.List;

public final class NotificationResponse {

    private NotificationResponse() {
    }

    public record Page(
            List<Info> notifications,
            boolean hasMore,
            Long nextCursor
    ) {
    }

    public record Info(
            Long id,
            String type,
            String title,
            String body,
            Instant occurredAt,
            boolean read
    ) {
    }

    public record UnreadCount(long unreadCount) {
    }

    public record MarkedCount(int markedCount) {
    }
}
