package online.lifeasgame.adminaudit.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.adminaudit.application.query.AdminAuditEventQuery;
import online.lifeasgame.adminaudit.application.result.AdminAuditQueryResult;
import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminAuditEventQuery query;

    public AdminAuditQueryResult.Page list(
            Long actorUserId,
            String action,
            String targetType,
            String targetId,
            AdminAuditResult result,
            String correlationId,
            Instant from,
            Instant to,
            String cursor,
            int size
    ) {
        validate(size, from, to);
        List<AdminAuditEventQuery.Row> rows = query.find(
                new AdminAuditEventQuery.Filter(
                        actorUserId,
                        action == null ? null : new AdminAuditAction(action),
                        targetType == null
                                ? null
                                : new AdminAuditTargetType(targetType),
                        targetId,
                        result,
                        correlationId,
                        from,
                        to,
                        AdminAuditCursor.decode(cursor)
                ),
                size + 1
        );
        boolean hasMore = rows.size() > size;
        List<AdminAuditQueryResult.Item> items = rows.stream()
                .limit(size)
                .map(AdminAuditQueryService::toItem)
                .toList();
        String nextCursor = hasMore && !items.isEmpty()
                ? AdminAuditCursor.encode(
                        items.getLast().occurredAt(),
                        items.getLast().id()
                )
                : null;
        return new AdminAuditQueryResult.Page(items, nextCursor);
    }

    private static void validate(int size, Instant from, Instant to) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    private static AdminAuditQueryResult.Item toItem(
            AdminAuditEventQuery.Row row
    ) {
        return new AdminAuditQueryResult.Item(
                row.id(),
                row.actorUserId(),
                row.action(),
                row.targetType(),
                row.targetId(),
                row.reason(),
                row.result().name(),
                row.correlationId(),
                row.idempotencyKey(),
                row.occurredAt()
        );
    }
}
