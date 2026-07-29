package online.lifeasgame.lifelog.application.result;

import online.lifeasgame.lifelog.domain.MediaLog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public final class MediaLogResult {

    private MediaLogResult() {
    }

    public record Created(
            Long id,
            Long lifeLogId,
            Instant recordedAt
    ) {
    }

    public record Info(
            Long id,
            Long playerId,
            String category,
            String title,
            String originalTitle,
            int currentEpisode,
            int totalEpisode,
            String status,
            Double rating,
            Set<String> tags,
            int rewatchCount,
            LocalDate startedOn,
            LocalDate finishedOn,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Info from(MediaLog mediaLog) {
            return new Info(
                    mediaLog.getId(), mediaLog.getPlayerId(),
                    mediaLog.getCategory().name(),
                    mediaLog.getTitle().value(),
                    mediaLog.getTitle().original(),
                    mediaLog.getProgress().current(), mediaLog.getProgress().total(),
                    mediaLog.getStatus().name(),
                    mediaLog.getRating().score(),
                    mediaLog.getMediaTags().values(),
                    mediaLog.getRewatchCount(),
                    mediaLog.getStartedOn(), mediaLog.getFinishedOn(),
                    mediaLog.getCreatedAt(), mediaLog.getUpdatedAt()
            );
        }
    }

    public record Deleted(Long mediaId) {
    }
}
