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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "stat_change_log",
        indexes = @Index(name = "idx_player_time2", columnList = "player_id,created_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class StatChangeLog extends AbstractTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @AttributeOverride(name = "value", column = @Column(name = "changes", columnDefinition = "json", nullable = false))
    @Embedded
    private JsonBlob changes;

    @Embedded
    private Reason reason;

    private StatChangeLog(Long playerId, JsonBlob changes, Reason reason) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.changes = Guard.notNull(changes, "changes");
        this.reason = Guard.notNull(reason, "reason");
    }

    public static StatChangeLog log(Long playerId, String changesJson, String reason) {
        return new StatChangeLog(playerId, JsonBlob.of(changesJson), Reason.of(reason));
    }
}
