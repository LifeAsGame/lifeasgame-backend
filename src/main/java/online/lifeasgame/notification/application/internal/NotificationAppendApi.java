package online.lifeasgame.notification.application.internal;

import java.time.Instant;
import online.lifeasgame.notification.domain.NotificationType;

public interface NotificationAppendApi {

    void append(AppendCommand command);

    record AppendCommand(
            Long playerId,
            String sourceEventId,
            NotificationType type,
            String title,
            String body,
            Instant occurredAt
    ) {
    }
}
