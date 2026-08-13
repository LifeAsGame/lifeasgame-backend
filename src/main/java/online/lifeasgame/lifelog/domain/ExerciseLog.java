package online.lifeasgame.lifelog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.LocalDate;

@AggregateRoot
@Entity
@Table(
        name = "exercise_logs",
        indexes = {
                @Index(name = "idx_exercise_player", columnList = "player_id"),
                @Index(name = "idx_exercise_category", columnList = "category"),
                @Index(name = "idx_exercise_exercised_on", columnList = "exercised_on")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseLog extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ExerciseCategory category;

    @Embedded
    private ExerciseMetrics metrics;

    @Column(name = "exercised_on", nullable = false)
    private LocalDate exercisedOn;

    @Column(name = "memo", length = 200)
    private String memo;

    private ExerciseLog(
            Long playerId,
            ExerciseCategory category,
            ExerciseMetrics metrics,
            LocalDate exercisedOn,
            String memo
    ) {
        Guard.notNull(playerId, "playerId");
        Guard.notNull(category, "category");
        Guard.notNull(metrics, "metrics");
        Guard.notNull(exercisedOn, "exercisedOn");
        this.playerId = playerId;
        this.category = category;
        this.metrics = metrics;
        this.exercisedOn = exercisedOn;
        this.memo = (memo == null || memo.isBlank()) ? null : memo.trim();
    }

    public static ExerciseLog create(
            Long playerId,
            ExerciseCategory category,
            ExerciseMetrics metrics,
            LocalDate exercisedOn,
            String memo
    ) {
        return new ExerciseLog(playerId, category, metrics, exercisedOn, memo);
    }

    public void update(
            ExerciseCategory category,
            ExerciseMetrics metrics,
            LocalDate exercisedOn,
            String memo
    ) {
        this.category = Guard.notNull(category, "category");
        this.metrics = Guard.notNull(metrics, "metrics");
        this.exercisedOn = Guard.notNull(exercisedOn, "exercisedOn");
        this.memo = (memo == null || memo.isBlank()) ? null : memo.trim();
    }
}
