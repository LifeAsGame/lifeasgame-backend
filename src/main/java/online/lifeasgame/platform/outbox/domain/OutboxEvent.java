package online.lifeasgame.platform.outbox.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.outbox.domain.error.OutboxError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

@Entity
@Table(
        name = "outbox_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_outbox_event_event_id",
                columnNames = "event_id"
        ),
        indexes = {
                @Index(
                        name = "idx_outbox_event_ready",
                        columnList = "status,next_attempt_at,id"
                ),
                @Index(
                        name = "idx_outbox_event_lease",
                        columnList = "status,locked_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends AbstractTime {

    public static final int MAX_EVENT_TYPE_LENGTH = 100;
    public static final int MAX_LOCKED_BY_LENGTH = 120;
    public static final int MAX_LAST_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "event_id",
            nullable = false,
            updatable = false,
            length = 36,
            columnDefinition = "char(36)"
    )
    private String eventId;

    @Column(name = "event_type", nullable = false, updatable = false, length = MAX_EVENT_TYPE_LENGTH)
    private String eventType;

    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", length = MAX_LOCKED_BY_LENGTH)
    private String lockedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = MAX_LAST_ERROR_LENGTH)
    private String lastError;

    private OutboxEvent(
            String eventId,
            String eventType,
            String payload,
            Instant occurredAt,
            Instant availableAt
    ) {
        this.eventId = requireText(eventId);
        this.eventType = requireText(eventType);
        this.payload = requireText(payload);
        this.occurredAt = requireInstant(occurredAt);
        this.status = OutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = requireInstant(availableAt);
    }

    public static OutboxEvent pending(
            String eventId,
            String eventType,
            String payload,
            Instant occurredAt,
            Instant availableAt
    ) {
        return new OutboxEvent(
                eventId,
                eventType,
                payload,
                occurredAt,
                availableAt
        );
    }

    public void claim(String instanceId, Instant claimedAt) {
        if (status != OutboxStatus.PENDING) {
            throw new DomainException(OutboxError.OUTBOX_EVENT_STATE_INVALID);
        }
        if (nextAttemptAt.isAfter(claimedAt)) {
            throw new DomainException(OutboxError.OUTBOX_EVENT_STATE_INVALID);
        }
        String owner = requireText(instanceId);
        if (owner.length() > MAX_LOCKED_BY_LENGTH) {
            throw new DomainException(OutboxError.OUTBOX_EVENT_LOCK_OWNER_MISMATCH);
        }
        status = OutboxStatus.PROCESSING;
        lockedAt = requireInstant(claimedAt);
        lockedBy = owner;
    }

    public void markPublished(String instanceId, Instant completedAt) {
        requireOwnedProcessing(instanceId);
        status = OutboxStatus.PUBLISHED;
        publishedAt = requireInstant(completedAt);
        nextAttemptAt = completedAt;
        lockedAt = null;
        lockedBy = null;
        lastError = null;
    }

    public void markFailed(
            String instanceId,
            int maxAttempts,
            Instant failedAt,
            Instant retryAt,
            String safeError
    ) {
        requireOwnedProcessing(instanceId);
        if (maxAttempts < 1) {
            throw new DomainException(OutboxError.OUTBOX_EVENT_STATE_INVALID);
        }

        attemptCount++;
        lastError = truncate(requireText(safeError), MAX_LAST_ERROR_LENGTH);
        lockedAt = null;
        lockedBy = null;

        if (attemptCount >= maxAttempts) {
            status = OutboxStatus.FAILED;
            nextAttemptAt = requireInstant(failedAt);
            return;
        }

        status = OutboxStatus.PENDING;
        nextAttemptAt = requireInstant(retryAt);
    }

    public void recoverExpiredLease(Instant recoveredAt) {
        if (status != OutboxStatus.PROCESSING) {
            throw new DomainException(OutboxError.OUTBOX_EVENT_STATE_INVALID);
        }
        status = OutboxStatus.PENDING;
        nextAttemptAt = requireInstant(recoveredAt);
        lockedAt = null;
        lockedBy = null;
    }

    private void requireOwnedProcessing(String instanceId) {
        if (status != OutboxStatus.PROCESSING) {
            throw new DomainException(OutboxError.OUTBOX_EVENT_STATE_INVALID);
        }
        if (!lockedBy.equals(instanceId)) {
            throw new DomainException(
                    OutboxError.OUTBOX_EVENT_LOCK_OWNER_MISMATCH
            );
        }
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(OutboxError.OUTBOX_EVENT_STATE_INVALID);
        }
        return value;
    }

    private static Instant requireInstant(Instant value) {
        if (value == null) {
            throw new DomainException(OutboxError.OUTBOX_EVENT_STATE_INVALID);
        }
        return value;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
