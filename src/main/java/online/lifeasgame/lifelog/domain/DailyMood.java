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
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;
import online.lifeasgame.shared.guard.Guard;

@Entity
@AggregateRoot
@Table(
        name = "daily_moods",
        indexes = {
                @Index(name = "idx_mood_player_day", columnList = "player_id,recorded_at"),
                @Index(name = "idx_mood_filters", columnList = "player_id,mood,energy,stress")
        },
        uniqueConstraints = @UniqueConstraint(name = "uq_mood_player_day", columnNames = {"player_id", "recorded_at"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DailyMood extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Scales mood = Scales.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Scales energy = Scales.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Scales stress = Scales.MEDIUM;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "daily_mood_tags",
            joinColumns = @JoinColumn(name = "daily_mood_id"),
            uniqueConstraints = @UniqueConstraint(name = "uq_daily_mood_tag", columnNames = {"daily_mood_id", "tag"})
    )
    private Set<Tag> tags = new HashSet<>();

    private DailyMood(Long playerId, Scales mood, Scales energy, Scales stress, Instant at) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.mood = mood == null ? Scales.MEDIUM : mood;
        this.energy = energy == null ? Scales.MEDIUM : energy;
        this.stress = stress == null ? Scales.MEDIUM : stress;
        this.recordedAt = at == null ? Instant.now() : at;
    }

    public static DailyMood record(Long playerId, Scales mood, Scales energy, Scales stress, Instant at) {
        return new DailyMood(playerId, mood, energy, stress, at);
    }

    public void update(Scales mood, Scales energy, Scales stress, String notes) {
        Guard.notNull(mood, "mood");
        Guard.notNull(energy, "energy");
        Guard.notNull(stress, "stress");
        this.mood = mood;
        this.energy = energy;
        this.stress = stress;
        this.notes = notes;
    }

    public void addTag(String t) {
        this.tags.add(Tag.of(t));
    }

    public void removeTag(String t) {
        this.tags.remove(Tag.of(t));
    }
}
