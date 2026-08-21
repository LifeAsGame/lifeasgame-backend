package online.lifeasgame.notification.api.mapper;

import online.lifeasgame.notification.api.response.NotificationResponse;
import online.lifeasgame.notification.application.result.NotificationResult;

public final class NotificationWebMapper {

    private NotificationWebMapper() {
    }

    public static NotificationResponse.Page toPage(
            NotificationResult.Page result
    ) {
        return new NotificationResponse.Page(
                result.notifications().stream()
                        .map(NotificationWebMapper::toInfo)
                        .toList(),
                result.hasMore(),
                result.nextCursor()
        );
    }

    public static NotificationResponse.UnreadCount toUnreadCount(long count) {
        return new NotificationResponse.UnreadCount(count);
    }

    public static NotificationResponse.MarkedCount toMarkedCount(int count) {
        return new NotificationResponse.MarkedCount(count);
    }

    private static NotificationResponse.Info toInfo(
            NotificationResult.Info result
    ) {
        return new NotificationResponse.Info(
                result.id(),
                result.type().name(),
                result.title(),
                result.body(),
                result.occurredAt(),
                result.read()
        );
    }
}
