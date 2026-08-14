package online.lifeasgame.lifelog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "media_logs", indexes = {
        @Index(name = "idx_media_player", columnList = "player_id"),
        @Index(name = "idx_media_category", columnList = "category"),
        @Index(name = "idx_media_status", columnList = "status")
}
)
@AggregateRoot
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaLog extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private MediaCategory category;

    @Embedded
    private Title title;

    @Embedded
    private EpisodeProgress progress;

    @Embedded
    private Rating rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WatchStatus status;

    @Embedded
    private MediaTags mediaTags;

    @Column(name = "rewatch_count", nullable = false)
    private int rewatchCount;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "finished_on")
    private LocalDate finishedOn;

    private MediaLog(
            Long playerId,
            MediaCategory category,
            Title title,
            EpisodeProgress progress,
            Rating rating,
            WatchStatus status,
            MediaTags mediaTags,
            int rewatchCount,
            LocalDate startedOn,
            LocalDate finishedOn
    ) {
        Guard.notNull(playerId, "playerId");
        Guard.notNull(category, "category");
        Guard.notNull(title, "title");
        Guard.notNull(progress, "progress");
        Guard.notNull(status, "status");
        Guard.minValue(rewatchCount, 0, "rewatchCount");

        this.playerId = playerId;
        this.category = category;
        this.title = title;
        this.progress = progress;
        this.rating = (rating == null) ? Rating.unrated() : rating;
        this.status = status;
        this.mediaTags = (mediaTags == null) ? MediaTags.of(null) : mediaTags;
        this.rewatchCount = rewatchCount;
        this.startedOn = startedOn;
        this.finishedOn = finishedOn;
    }

    public static MediaLog create(
            Long playerId,
            MediaCategory category,
            Title title,
            EpisodeProgress progress,
            WatchStatus status,
            MediaTags mediaTags
    ) {
        return new MediaLog(
                playerId,
                category,
                title,
                progress,
                Rating.unrated(),
                status,
                mediaTags,
                0,
                null,
                null
        );
    }

    public void rate(double score) {
        this.rating = Rating.of(score);
    }

    public void advanceEpisode(Integer step) {
        int s = (step == null) ? 1 : step;
        Guard.minValue(s, 1, "step");
        this.progress = this.progress.advance(s);

        if (this.progress.completed()) {
            this.status = WatchStatus.COMPLETED;
            if (this.finishedOn == null) {
                this.finishedOn = LocalDate.now();
            }
        } else if (this.status == WatchStatus.PLANNED) {
            this.status = WatchStatus.WATCHING;
        }
    }

    public void markStatus(WatchStatus status) {
        Guard.notNull(status, "status");
        this.status = status;
        if (status == WatchStatus.COMPLETED && !this.progress.completed()) {
            this.progress = EpisodeProgress.of(this.progress.total(), this.progress.total());
            if (this.finishedOn == null) {
                this.finishedOn = LocalDate.now();
            }
        }
    }

    public void rewatch() {
        this.rewatchCount += 1;
    }

    public void update(
            MediaCategory mediaCategory,
            Title title,
            EpisodeProgress episodeProgress,
            WatchStatus watchStatus,
            MediaTags tags
    ) {
        Guard.notNull(mediaCategory, "mediaCategory");
        Guard.notNull(title, "title");
        Guard.notNull(episodeProgress, "episodeProgress");
        Guard.notNull(watchStatus, "watchStatus");
        Guard.notNull(tags, "tags");

        this.category = mediaCategory;
        this.title = title;
        this.progress = episodeProgress;
        this.status = watchStatus;
        this.mediaTags = tags;
    }
}
