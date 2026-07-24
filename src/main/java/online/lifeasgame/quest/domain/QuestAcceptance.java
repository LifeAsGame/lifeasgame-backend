package online.lifeasgame.quest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.quest.domain.error.QuestError;

import java.time.Instant;

@Getter
@Entity
@AggregateRoot
@Table(
        name = "quest_acceptances",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_repeat",
                columnNames = {"player_id", "quest_id", "period_start", "period_end"}
        ),
        indexes = {
                @Index(name = "idx_qa_player", columnList = "player_id"),
                @Index(name = "idx_qa_quest", columnList = "quest_id"),
                @Index(name = "idx_qa_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestAcceptance extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "goal_reached_at")
    private Instant goalReachedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Version
    private Long version;

    private QuestAcceptance(
            Long questId,
            Long playerId,
            Long partyId,
            Long guildId,
            TimePeriod period
    ) {
        Guard.notNull(questId, "questId");
        Guard.minValue(questId, 1L, "questId");
        Guard.notNull(playerId, "playerId");
        Guard.minValue(playerId, 1L, "playerId");
        Guard.notNull(period, "period");
        this.questId = questId;
        this.playerId = playerId;
        this.partyId = partyId;
        this.guildId = guildId;
        this.period = period;
        this.progressValue = 0;
    }

    public static QuestAcceptance start(Long questId, Long playerId, TimePeriod period) {
        return new QuestAcceptance(questId, playerId, null, null, period);
    }

    public static QuestAcceptance start(Long questId, Long playerId, Long partyId, Long guildId, TimePeriod period) {
        return new QuestAcceptance(questId, playerId, partyId, guildId, period);
    }

    public void addProgress(int delta, Quest quest) {
        addProgress(delta, quest, Instant.now());
    }

    public void addProgress(int delta, Quest quest, Instant reachedAt) {
        assertProgressAllowed();
        Guard.notNull(quest, "quest");
        Guard.minValue(delta, 0, "delta");
        this.progressValue += delta;
        if (quest.target().reachedBy(this.progressValue)) {
            reachGoal(reachedAt);
        }
    }

    public void setProgress(int value, Quest quest) {
        setProgress(value, quest, Instant.now());
    }

    public void setProgress(int value, Quest quest, Instant reachedAt) {
        assertProgressAllowed();
        Guard.notNull(quest, "quest");
        Guard.minValue(value, 0, "progress value");
        this.progressValue = value;
        if (quest.target().reachedBy(this.progressValue)) {
            reachGoal(reachedAt);
        }
    }

    public boolean reachGoal(Instant reachedAt) {
        if (status == QuestStatus.GOAL_REACHED) {
            return false;
        }
        if (status != QuestStatus.IN_PROGRESS) {
            throw new DomainException(QuestError.QUEST_ACCEPTANCE_GOAL_REACH_NOT_ALLOWED);
        }
        this.goalReachedAt = requireTransitionTime(reachedAt);
        this.status = QuestStatus.GOAL_REACHED;
        return true;
    }

    public boolean complete(Instant completedAt) {
        if (status == QuestStatus.COMPLETED) {
            return false;
        }
        if (status != QuestStatus.GOAL_REACHED) {
            throw new DomainException(QuestError.QUEST_ACCEPTANCE_COMPLETION_NOT_ALLOWED);
        }
        this.completedAt = requireTransitionTime(completedAt);
        this.status = QuestStatus.COMPLETED;
        return true;
    }

    public boolean cancel() {
        if (status == QuestStatus.CANCELED) {
            return false;
        }
        if (status == QuestStatus.COMPLETED) {
            throw new DomainException(QuestError.QUEST_ACCEPTANCE_CANCELLATION_NOT_ALLOWED);
        }
        this.status = QuestStatus.CANCELED;
        return true;
    }

    public boolean changeStatus(QuestStatus questStatus, Instant changedAt) {
        if (questStatus == null) {
            throw new DomainException(QuestError.INVALID_QUEST_STATUS);
        }
        if (this.status == questStatus) {
            return false;
        }
        return switch (questStatus) {
            case GOAL_REACHED -> reachGoal(changedAt);
            case COMPLETED -> complete(changedAt);
            case CANCELED -> cancel();
            case IN_PROGRESS ->
                    throw new DomainException(
                            QuestError.QUEST_ACCEPTANCE_STATUS_TRANSITION_NOT_ALLOWED
                    );
        };
    }

    public boolean isInProgress() {
        return status == QuestStatus.IN_PROGRESS;
    }

    public boolean isGoalReached() {
        return status == QuestStatus.GOAL_REACHED;
    }

    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED;
    }

    public boolean isCanceled() {
        return status == QuestStatus.CANCELED;
    }

    public void assignIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        this.idempotencyKey = key;
    }

    private void assertProgressAllowed() {
        if (status != QuestStatus.IN_PROGRESS) {
            throw new DomainException(QuestError.QUEST_ACCEPTANCE_PROGRESS_NOT_ALLOWED);
        }
    }

    private Instant requireTransitionTime(Instant transitionTime) {
        if (transitionTime == null) {
            throw new DomainException(QuestError.QUEST_TRANSITION_TIME_REQUIRED);
        }
        return transitionTime;
    }
}
