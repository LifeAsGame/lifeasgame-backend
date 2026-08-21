package online.lifeasgame.notification.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.notification.application.query.NotificationInboxQuery;
import online.lifeasgame.notification.application.result.NotificationResult;
import online.lifeasgame.notification.domain.error.NotificationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationInboxQuery query;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public NotificationResult.Page inbox(Long cursor, int size) {
        validateSize(size);
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<NotificationInboxQuery.Row> rows = query.findInbox(
                playerId,
                cursor,
                size + 1
        );
        boolean hasMore = rows.size() > size;
        List<NotificationResult.Info> notifications = rows.stream()
                .limit(size)
                .map(NotificationQueryService::toInfo)
                .toList();
        Long nextCursor = hasMore && !notifications.isEmpty()
                ? notifications.getLast().id()
                : null;
        return new NotificationResult.Page(
                notifications,
                hasMore,
                nextCursor
        );
    }

    public long unreadCount() {
        return query.countUnread(
                currentPlayerAccessor.currentPlayerIdOrThrow()
        );
    }

    private static NotificationResult.Info toInfo(
            NotificationInboxQuery.Row row
    ) {
        return new NotificationResult.Info(
                row.id(),
                row.type(),
                row.title(),
                row.body(),
                row.occurredAt(),
                row.readAt() != null
        );
    }

    private static void validateSize(int size) {
        if (size < 1 || size > 100) {
            throw new DomainException(NotificationError.PAGE_SIZE_INVALID);
        }
    }
}
