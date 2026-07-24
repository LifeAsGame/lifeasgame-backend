package online.lifeasgame.quest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "quest_signal_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quest_signal_receipt_identity",
                columnNames = {"quest_code", "player_id", "correlation_id"}
        ),
        indexes = {
                @Index(name = "idx_quest_signal_receipt_player", columnList = "player_id"),
                @Index(name = "idx_quest_signal_receipt_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestSignalReceipt extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quest_code", length = 80, nullable = false)
    private String questCode;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "correlation_id", length = 120, nullable = false)
    private String correlationId;

    @Column(name = "signal_type", length = 30, nullable = false)
    private String signalType;

    @Column(
            name = "payload_fingerprint",
            length = 64,
            nullable = false,
            columnDefinition = "CHAR(64)"
    )
    private String payloadFingerprint;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    private QuestSignalReceipt(
            String questCode,
            Long playerId,
            String correlationId,
            String signalType,
            String payloadFingerprint,
            Instant occurredAt
    ) {
        this.questCode = Guard.notBlank(questCode, "questCode").trim();
        this.playerId = Guard.notNull(playerId, "playerId");
        Guard.minValue(playerId, 1L, "playerId");
        this.correlationId = Guard.notBlank(correlationId, "correlationId").trim();
        this.signalType = Guard.notBlank(signalType, "signalType").trim();
        this.payloadFingerprint =
                Guard.notBlank(payloadFingerprint, "payloadFingerprint").trim();
        this.occurredAt = Guard.notNull(occurredAt, "occurredAt");
    }

    public static QuestSignalReceipt create(
            String questCode,
            Long playerId,
            String correlationId,
            String signalType,
            String payloadFingerprint,
            Instant occurredAt
    ) {
        return new QuestSignalReceipt(
                questCode,
                playerId,
                correlationId,
                signalType,
                payloadFingerprint,
                occurredAt
        );
    }

    public boolean hasFingerprint(String fingerprint) {
        return payloadFingerprint.equals(fingerprint);
    }
}
