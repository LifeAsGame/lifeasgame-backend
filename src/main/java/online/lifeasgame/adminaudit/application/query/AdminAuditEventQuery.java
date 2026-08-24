package online.lifeasgame.adminaudit.application.query;

import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;

import java.time.Instant;
import java.util.List;

public interface AdminAuditEventQuery {

    List<Row> find(Filter filter, int limit);

    record Filter(
            Long actorUserId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            String targetId,
            AdminAuditResult result,
            String correlationId,
            Instant from,
            Instant to,
            Cursor cursor
    ) {
    }

    record Cursor(Instant occurredAt, Long id) {
    }

    record Row(
            Long id,
            Long actorUserId,
            String action,
            String targetType,
            String targetId,
            String reason,
            AdminAuditResult result,
            String correlationId,
            String idempotencyKey,
            Instant occurredAt
    ) {
    }
}
