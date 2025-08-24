package online.lifeasgame.quest.domain;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "quest_acceptances",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_repeat",
                columnNames = {"player_id", "quest_id", "period_start", "period_end"}
        ),
        indexes = {
                @Index(name="idx_qa_player", columnList="player_id"),
                @Index(name="idx_qa_quest",  columnList="quest_id"),
                @Index(name="idx_qa_status", columnList="status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestAcceptance extends AbstractTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quest_id", nullable = false)
    private Long questId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "guild_id")
    private Long guildId;

    @Embedded
    private TimePeriod period;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private QuestStatus status = QuestStatus.IN_PROGRESS;

    @Column(name = "progress_value", nullable = false)
    private int progressValue = 0;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Version
    private Long version;

    private QuestAcceptance(Long questId, Long playerId, TimePeriod period) {
        this.questId = questId;
        this.playerId = playerId;
        this.period = period;
        this.progressValue = 0;
    }

    public static QuestAcceptance start(Long questId, Long playerId, TimePeriod period) {
        return new QuestAcceptance(questId, playerId, period);
    }

    public void addProgress(int delta, Quest quest){
        Guard.checkState(status == QuestStatus.IN_PROGRESS, "Quest status is not in progress");
        Guard.minValue(delta, 0, "delta");
        this.progressValue += delta;
        if (quest.target().reachedBy(this.progressValue)) {
            complete();
        }
    }

    public void setProgress(int value, Quest quest){
        Guard.checkState(status == QuestStatus.IN_PROGRESS, "Quest status is not in progress");
        Guard.minValue(value, 0, "progress value");
        this.progressValue = value;
        if (quest.target().reachedBy(this.progressValue)) {
            complete();
        }
    }

    public void complete(){
        if (status == QuestStatus.DONE) return;
        Guard.checkState(status == QuestStatus.IN_PROGRESS, "cannot complete from" + status);
        this.status = QuestStatus.DONE;
        this.completedAt = Instant.now();
    }

    public void cancel(){
        Guard.checkState(status != QuestStatus.DONE, "cannot cancel done quest");
        this.status = QuestStatus.CANCELED;
    }
}
