package online.lifeasgame.logs.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "exp_history",
        indexes = @Index(name = "idx_player_time", columnList = "player_id,created_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpHistory extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 24, nullable = false)
    private ExpReason reason;

    @AttributeOverride(name = "value", column = @Column(name = "context", columnDefinition = "json"))
    @Embedded
    private JsonBlob context;

    private ExpHistory(Long playerId, int delta, ExpReason reason, JsonBlob context) {
        this.playerId = Guard.notNull(playerId, "playerId");
        Guard.checkState(delta != 0, "delta must not be 0");
        this.delta = delta;
        this.reason = Guard.notNull(reason, "reason");
        this.context = context;
    }

    public static ExpHistory record(Long playerId, int delta, ExpReason reason, String contextJson) {
        return new ExpHistory(playerId, delta, reason, contextJson == null ? null : JsonBlob.of(contextJson));
    }
}
