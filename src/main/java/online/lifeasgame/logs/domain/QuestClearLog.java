package online.lifeasgame.logs.domain;

import jakarta.persistence.Column;
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
        name = "quest_clear_log",
        indexes = @Index(name = "idx_player_quest", columnList = "player_id,quest_id,completed_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestClearLog extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "quest_id", nullable = false)
    private Long questId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    private QuestClearLog(Long playerId, Long questId, Instant at) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.questId = Guard.notNull(questId, "questId");
        this.completedAt = (at == null) ? Instant.now() : at;
    }

    public static QuestClearLog complete(Long playerId, Long questId, Instant at) {
        return new QuestClearLog(playerId, questId, at);
    }
}
