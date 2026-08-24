package online.lifeasgame.adminaudit.api.response;

import java.time.Instant;
import java.util.List;

public final class AdminAuditResponse {

    private AdminAuditResponse() {
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
