package online.lifeasgame.adminaudit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
@Entity
@Table(
        name = "admin_audit_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_admin_audit_action_idempotency",
                columnNames = {"action", "idempotency_key"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditEvent {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._:-]*"
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 128)
    private String targetId;

    @Column(length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AdminAuditResult result;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static AdminAuditEvent record(
            Long actorUserId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            String targetId,
            String reason,
            AdminAuditResult result,
            String correlationId,
            String idempotencyKey,
            Instant occurredAt
    ) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId must be positive");
        }

        AdminAuditEvent event = new AdminAuditEvent();
        event.actorUserId = actorUserId;
        event.action = Objects.requireNonNull(action, "action").value();
        event.targetType = Objects.requireNonNull(
                targetType,
                "targetType"
        ).value();
        event.targetId = requireIdentifier(targetId, "targetId", 128);
        event.reason = optionalReason(reason);
        event.result = Objects.requireNonNull(result, "result");
        event.correlationId = requireIdentifier(
                correlationId,
                "correlationId",
                100
        );
        event.idempotencyKey = optionalIdentifier(
                idempotencyKey,
                "idempotencyKey",
                128
        );
        event.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        return event;
    }

    private static String requireText(String value, String field, int max) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(
                    field + " must contain 1-" + max + " characters"
            );
        }
        return value;
    }

    private static String optionalReason(String value) {
        if (value == null) {
            return null;
        }
        if (value.codePoints().anyMatch(AdminAuditEvent::isUnsafeReasonCharacter)) {
            throw new IllegalArgumentException(
                    "reason must be a single-line operational rationale"
            );
        }
        return requireText(value.strip(), "reason", 512);
    }

    private static boolean isUnsafeReasonCharacter(int value) {
        int type = Character.getType(value);
        return Character.isISOControl(value)
                || type == Character.FORMAT
                || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR;
    }

    private static String requireIdentifier(
            String value,
            String field,
            int max
    ) {
        String validated = requireText(value, field, max);
        if (!SAFE_IDENTIFIER.matcher(validated).matches()) {
            throw new IllegalArgumentException(field + " has an unsafe format");
        }
        return validated;
    }

    private static String optionalIdentifier(
            String value,
            String field,
            int max
    ) {
        return value == null ? null : requireIdentifier(value, field, max);
    }
}
