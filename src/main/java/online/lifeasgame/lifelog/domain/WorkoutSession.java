package online.lifeasgame.lifelog.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "workout_sessions",
        indexes = {
                @Index(name = "idx_workout_player_time", columnList = "player_id,started_at"),
                @Index(name = "idx_workout_type", columnList = "player_id,type")}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSession extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private WorkoutType type;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_min")
    private Integer durationMin;

    private Double distanceKm;

    private Integer calories;

    @Column(name = "rpe")
    private Integer rpe;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "workout_session_tags",
            joinColumns = @JoinColumn(name = "workout_session_id"),
            uniqueConstraints = @UniqueConstraint(name = "uq_workout_session_tag", columnNames = {"workout_session_id", "tag"})
    )
    private Set<Tag> tags = new HashSet<>();

    private WorkoutSession(Long playerId, WorkoutType type, Instant startedAt) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.type = Guard.notNull(type, "type");
        this.startedAt = startedAt == null ? Instant.now() : startedAt;
    }

    public static WorkoutSession start(Long playerId, WorkoutType type, Instant startedAt) {
        return new WorkoutSession(playerId, type, startedAt);
    }

    public void end(Instant when) {
        Guard.checkState(this.endedAt == null, "already ended");
        Instant end = when == null ? Instant.now() : when;
        Guard.checkState(!end.isBefore(startedAt), "endedAt before startedAt");
        this.endedAt = end;
        long minutes = Math.max(0, (end.toEpochMilli() - startedAt.toEpochMilli()) / 60000);
        this.durationMin = (int) Math.min(Integer.MAX_VALUE, minutes);
    }

    public void metrics(Double distanceKm, Integer calories, Integer rpe) {
        if (distanceKm != null) {
            Guard.check(distanceKm >= 0.0, "distanceKm must be >= 0");
        }
        if (calories != null) {
            Guard.minValue(calories, 0, "calories");
        }
        if (rpe != null) {
            Guard.inRange(rpe, 0, 10, "rpe 0~10");
        }
        this.distanceKm = distanceKm;
        this.calories = calories;
        this.rpe = rpe;
    }

    public void note(String notes) { this.notes = notes; }

    public void addTag(String t) { this.tags.add(Tag.of(t)); }
    public void removeTag(String t) { this.tags.remove(Tag.of(t)); }
}
