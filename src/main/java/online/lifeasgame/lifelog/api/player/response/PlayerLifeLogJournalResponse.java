package online.lifeasgame.lifelog.api.player.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class PlayerLifeLogJournalResponse {

    private PlayerLifeLogJournalResponse() {
    }

    public record Page(
            List<Entry> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record Entry(
            Long lifeLogId,
            String sourceType,
            Long sourceId,
            String subtype,
            String entryMode,
            String reflectionScope,
            String periodKey,
            Long primaryRoleId,
            Long roleEventId,
            Instant recordedAt,
            Preview preview
    ) {
    }

    public record Detail(
            Long lifeLogId,
            String sourceType,
            Long sourceId,
            String subtype,
            String entryMode,
            String reflectionScope,
            String periodKey,
            Long primaryRoleId,
            Long roleEventId,
            Instant recordedAt,
            Source source
    ) {
    }

    public sealed interface Preview permits
            CollectionPreview,
            ExercisePreview,
            MediaPreview {
    }

    public record CollectionPreview(
            String category,
            String title,
            Integer quantity
    ) implements Preview {
    }

    public record ExercisePreview(
            String category,
            Integer durationMinutes,
            Double distanceKm,
            Integer calories,
            LocalDate exercisedOn,
            String memo
    ) implements Preview {
    }

    public record MediaPreview(
            String category,
            String title,
            Integer currentEpisode,
            Integer totalEpisode,
            String status,
            Double rating
    ) implements Preview {
    }

    public sealed interface Source permits
            CollectionSource,
            ExerciseSource,
            MediaSource {
    }

    public record CollectionSource(
            String category,
            String title,
            String originalTitle,
            Integer quantity,
            String conditionNote,
            String acquiredFrom,
            Set<String> tags,
            Instant createdAt,
            Instant updatedAt
    ) implements Source {
    }

    public record ExerciseSource(
            String category,
            Integer durationMinutes,
            Double distanceKm,
            Integer calories,
            LocalDate exercisedOn,
            String memo,
            Instant createdAt,
            Instant updatedAt
    ) implements Source {
    }

    public record MediaSource(
            String category,
            String title,
            String originalTitle,
            Integer currentEpisode,
            Integer totalEpisode,
            String status,
            Double rating,
            Set<String> tags,
            int rewatchCount,
            LocalDate startedOn,
            LocalDate finishedOn,
            Instant createdAt,
            Instant updatedAt
    ) implements Source {
    }
}
