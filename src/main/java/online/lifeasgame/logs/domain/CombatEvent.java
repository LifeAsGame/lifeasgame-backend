package online.lifeasgame.logs.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "combat_events",
        indexes = @Index(name = "idx_player_time4", columnList = "player_id,occurred_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CombatEvent extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "event_type", length = 30, nullable = false)
    private String eventType;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "payload", columnDefinition = "json", nullable = false))
    private JsonBlob payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    private CombatEvent(Long playerId, CombatEventType type, JsonBlob payload, Instant occurredAt) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.eventType = String.valueOf(Guard.notNull(type, "eventType"));
        this.payload = Guard.notNull(payload, "payload");
        this.occurredAt = (occurredAt == null) ? Instant.now() : occurredAt;
    }

    public static CombatEvent of(Long playerId, CombatEventType type, String payloadJson, Instant occurredAt) {
        return new CombatEvent(playerId, type, JsonBlob.of(payloadJson), occurredAt);
    }
}
