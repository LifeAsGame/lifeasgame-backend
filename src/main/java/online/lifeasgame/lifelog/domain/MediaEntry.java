package online.lifeasgame.lifelog.domain;


import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
        name = "media_entries",
        indexes = {
                @Index(name = "idx_media_player_done", columnList = "player_id,finished_at"),
                @Index(name = "idx_media_filter", columnList = "player_id,kind,status")}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaEntry extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private MediaKind kind;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private MediaStatus status = MediaStatus.PLANNED;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(length = 120)
    private String creator;

    @Column(length = 80)
    private String platform;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Embedded
    private Rating rating = Rating.of(null);

    @Lob
    private String review;

    @Column(name = "metadata", columnDefinition = "json")
    private String metadataJson;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "media_entry_tags",
            joinColumns = @JoinColumn(name = "media_entry_id"),
            uniqueConstraints = @UniqueConstraint(name = "uq_media_entry_tag", columnNames = {"media_entry_id", "tag"})
    )
    private Set<Tag> tags = new HashSet<>();

    private MediaEntry(Long playerId, MediaKind kind, String title) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.kind = Guard.notNull(kind, "kind");
        this.title = Guard.notBlank(title, "title");
    }

    public static MediaEntry plan(Long playerId, MediaKind kind, String title) {
        return new MediaEntry(playerId, kind, title);
    }

    public void start(Instant when) {
        Guard.checkState(status == MediaStatus.PLANNED || status == MediaStatus.DROPPED, "cannot start from current status");
        this.status = MediaStatus.IN_PROGRESS;
        this.startedAt = when == null ? Instant.now() : when;
    }

    public void finish(Instant when) {
        Guard.checkState(status == MediaStatus.IN_PROGRESS, "finish only from IN_PROGRESS");
        Instant end = when == null ? Instant.now() : when;
        Guard.check(startedAt != null && !end.isBefore(startedAt),
                "cannot finish before start or without startedAt");
        this.status = MediaStatus.DONE;
        this.finishedAt = end;
    }

    public void drop() { this.status = MediaStatus.DROPPED; }

    public void rate(Integer v) { this.rating = Rating.of(v); }
    public void writeReview(String text) { this.review = text; }

    public void addTag(String t) { this.tags.add(Tag.of(t)); }
    public void removeTag(String t) { this.tags.remove(Tag.of(t)); }
}
