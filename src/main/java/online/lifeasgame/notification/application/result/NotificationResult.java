package online.lifeasgame.notification.application.result;

import java.time.Instant;
import java.util.List;

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
            String type,
            String title,
            String body,
            String titleCopyId,
            Integer titleCopyVersion,
            String bodyCopyId,
            Integer bodyCopyVersion,
            String copyLocale,
            Instant occurredAt,
            boolean read
    ) {
    }
}
