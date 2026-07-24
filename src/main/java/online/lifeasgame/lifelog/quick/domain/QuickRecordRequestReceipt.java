package online.lifeasgame.lifelog.quick.domain;

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
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.quick.domain.error.QuickRecordError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "quick_record_request_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quick_record_request_receipt_identity",
                columnNames = {"player_id", "idempotency_key"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuickRecordRequestReceipt extends AbstractTime {

    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 128;
    public static final int REQUEST_HASH_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(
            name = "idempotency_key",
            nullable = false,
            length = IDEMPOTENCY_KEY_MAX_LENGTH
    )
    private String idempotencyKey;

    @Column(
            name = "request_hash",
            nullable = false,
            length = REQUEST_HASH_LENGTH,
            columnDefinition = "CHAR(64)"
    )
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20)
    private LifeLogType sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    private QuickRecordRequestReceipt(
            Long playerId,
            String idempotencyKey,
            String requestHash
    ) {
        this.playerId = Guard.notNull(playerId, "playerId");
        Guard.minValue(playerId, 1L, "playerId");
        this.idempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        this.requestHash = requireHash(requestHash);
    }

    public static QuickRecordRequestReceipt reserve(
            Long playerId,
            String idempotencyKey,
            String requestHash
    ) {
        return new QuickRecordRequestReceipt(
                playerId,
                idempotencyKey,
                requestHash
        );
    }

    public static String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(
                    QuickRecordError.IDEMPOTENCY_KEY_REQUIRED
            );
        }
        String normalized = value.trim();
        if (normalized.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw invalid();
        }
        return normalized;
    }

    public void assertRequestHash(String candidate) {
        if (!requestHash.equals(requireHash(candidate))) {
            throw new DomainException(
                    QuickRecordError.IDEMPOTENCY_KEY_PAYLOAD_CONFLICT
            );
        }
    }

    public boolean isCompleted() {
        return sourceType != null
                && sourceId != null
                && recordedAt != null;
    }

    public void complete(
            LifeLogType sourceType,
            Long sourceId,
            Instant recordedAt
    ) {
        if (isCompleted()
                || sourceType == null
                || sourceId == null
                || sourceId <= 0
                || recordedAt == null) {
            throw invalid();
        }
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.recordedAt = recordedAt;
    }

    public StoredResult replay(String candidateHash) {
        assertRequestHash(candidateHash);
        return storedResult();
    }

    public StoredResult storedResult() {
        if (!isCompleted()) {
            throw invalid();
        }
        return new StoredResult(sourceType, sourceId, recordedAt);
    }

    private static String requireHash(String value) {
        if (value == null
                || value.length() != REQUEST_HASH_LENGTH
                || !value.matches("[0-9a-f]{64}")) {
            throw invalid();
        }
        return value;
    }

    private static DomainException invalid() {
        return new DomainException(QuickRecordError.INVALID_REQUEST);
    }

    public record StoredResult(
            LifeLogType sourceType,
            Long sourceId,
            Instant recordedAt
    ) {
    }
}
