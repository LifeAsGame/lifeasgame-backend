package online.lifeasgame.adminaudit.application.internal;

import online.lifeasgame.adminaudit.domain.AdminAuditAction;
import online.lifeasgame.adminaudit.domain.AdminAuditResult;
import online.lifeasgame.adminaudit.domain.AdminAuditTargetType;

import java.time.Instant;

/**
 * Required Admin accountability boundary for high-risk commands.
 *
 * <p>Callers must invoke {@link #append(AppendCommand)} inside the same
 * transaction as the successful business mutation and let every append
 * exception propagate. Reason is nullable only for this foundation; Unit E
 * must require it for each approved high-risk command.</p>
 */
public interface AdminAuditInternalApi {

    AppendResult append(AppendCommand command);

    record AppendCommand(
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            String targetId,
            String reason,
            AdminAuditResult result,
            String correlationId,
            String idempotencyKey
    ) {
    }

    record AppendResult(Long auditEventId, Instant occurredAt) {
    }
}
