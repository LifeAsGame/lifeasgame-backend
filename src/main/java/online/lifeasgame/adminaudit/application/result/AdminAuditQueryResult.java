package online.lifeasgame.adminaudit.application.result;

import java.time.Instant;
import java.util.List;

public final class AdminAuditQueryResult {

    private AdminAuditQueryResult() {
    }

    public record Page(List<Item> items, String nextCursor) {
    }

    public record Item(
            Long id,
            Long actorUserId,
            String action,
            String targetType,
            String targetId,
            String reason,
            String result,
            String correlationId,
            String idempotencyKey,
            Instant occurredAt
    ) {
    }
}
