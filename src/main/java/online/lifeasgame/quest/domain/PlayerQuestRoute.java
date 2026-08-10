package online.lifeasgame.quest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.quest.domain.error.QuestError;

import java.time.Instant;

@Getter
@Entity
@AggregateRoot
@Table(
        name = "player_quest_routes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_player_quest_route",
                columnNames = {"player_id", "route_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerQuestRoute extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "current_step_id", nullable = false)
    private Long currentStepId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PlayerQuestRouteStatus status;

    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private Long version;

    private PlayerQuestRoute(
            Long playerId,
            Long routeId,
            Long currentStepId,
            Instant selectedAt
    ) {
        if (invalidId(playerId)
                || invalidId(routeId)
                || invalidId(currentStepId)
                || selectedAt == null) {
            throw new DomainException(QuestError.ROUTE_DEFINITION_INVALID);
        }
        this.playerId = playerId;
        this.routeId = routeId;
        this.currentStepId = currentStepId;
        this.status = PlayerQuestRouteStatus.IN_PROGRESS;
        this.selectedAt = selectedAt;
    }

    public static PlayerQuestRoute start(
            Long playerId,
            Long routeId,
            Long firstStepId,
            Instant selectedAt
    ) {
        return new PlayerQuestRoute(playerId, routeId, firstStepId, selectedAt);
    }

    public void advanceTo(Long expectedStepId, Long nextStepId) {
        assertCanAdvance(expectedStepId);
        if (invalidId(nextStepId) || currentStepId.equals(nextStepId)) {
            throw new DomainException(QuestError.ROUTE_STEP_NOT_CURRENT);
        }
        currentStepId = nextStepId;
    }

    public void complete(Long expectedStepId, Instant completedAt) {
        assertCanAdvance(expectedStepId);
        if (completedAt == null) {
            throw new DomainException(QuestError.ROUTE_DEFINITION_INVALID);
        }
        status = PlayerQuestRouteStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public boolean isCompleted() {
        return status == PlayerQuestRouteStatus.COMPLETED;
    }

    private void assertCanAdvance(Long expectedStepId) {
        if (isCompleted()) {
            throw new DomainException(QuestError.ROUTE_ALREADY_COMPLETED);
        }
        if (expectedStepId == null || !currentStepId.equals(expectedStepId)) {
            throw new DomainException(QuestError.ROUTE_STEP_NOT_CURRENT);
        }
    }

    private static boolean invalidId(Long value) {
        return value == null || value <= 0;
    }
}
