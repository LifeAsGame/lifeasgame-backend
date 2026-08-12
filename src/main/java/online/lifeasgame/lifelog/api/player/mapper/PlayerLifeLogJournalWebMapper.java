package online.lifeasgame.lifelog.api.player.mapper;

import online.lifeasgame.lifelog.api.player.response.PlayerLifeLogJournalResponse;
import online.lifeasgame.lifelog.application.result.LifeLogJournalResult;

public final class PlayerLifeLogJournalWebMapper {

    private PlayerLifeLogJournalWebMapper() {
    }

    public static PlayerLifeLogJournalResponse.Page toPage(
            LifeLogJournalResult.Page result
    ) {
        return new PlayerLifeLogJournalResponse.Page(
                result.content().stream()
                        .map(PlayerLifeLogJournalWebMapper::toEntry)
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public static PlayerLifeLogJournalResponse.Detail toDetail(
            LifeLogJournalResult.Detail result
    ) {
        return new PlayerLifeLogJournalResponse.Detail(
                result.lifeLogId(),
                result.sourceType(),
                result.sourceId(),
                result.subtype(),
                result.entryMode(),
                result.reflectionScope(),
                result.periodKey(),
                result.primaryRoleId(),
                result.roleEventId(),
                result.recordedAt(),
                toSource(result.source())
        );
    }

    private static PlayerLifeLogJournalResponse.Entry toEntry(
            LifeLogJournalResult.Entry result
    ) {
        return new PlayerLifeLogJournalResponse.Entry(
                result.lifeLogId(),
                result.sourceType(),
                result.sourceId(),
                result.subtype(),
                result.entryMode(),
                result.reflectionScope(),
                result.periodKey(),
                result.primaryRoleId(),
                result.roleEventId(),
                result.recordedAt(),
                toPreview(result.preview())
        );
    }

    private static PlayerLifeLogJournalResponse.Preview toPreview(
            LifeLogJournalResult.Preview preview
    ) {
        return switch (preview) {
            case LifeLogJournalResult.CollectionPreview value ->
                    new PlayerLifeLogJournalResponse.CollectionPreview(
                            value.category(),
                            value.title(),
                            value.quantity()
                    );
            case LifeLogJournalResult.ExercisePreview value ->
                    new PlayerLifeLogJournalResponse.ExercisePreview(
                            value.category(),
                            value.durationMinutes(),
                            value.distanceKm(),
                            value.calories(),
                            value.exercisedOn(),
                            value.memo()
                    );
            case LifeLogJournalResult.MediaPreview value ->
                    new PlayerLifeLogJournalResponse.MediaPreview(
                            value.category(),
                            value.title(),
                            value.currentEpisode(),
                            value.totalEpisode(),
                            value.status(),
                            value.rating()
                    );
        };
    }

    private static PlayerLifeLogJournalResponse.Source toSource(
            LifeLogJournalResult.Source source
    ) {
        return switch (source) {
            case LifeLogJournalResult.CollectionSource value ->
                    new PlayerLifeLogJournalResponse.CollectionSource(
                            value.category(),
                            value.title(),
                            value.originalTitle(),
                            value.quantity(),
                            value.conditionNote(),
                            value.acquiredFrom(),
                            value.tags(),
                            value.createdAt(),
                            value.updatedAt()
                    );
            case LifeLogJournalResult.ExerciseSource value ->
                    new PlayerLifeLogJournalResponse.ExerciseSource(
                            value.category(),
                            value.durationMinutes(),
                            value.distanceKm(),
                            value.calories(),
                            value.exercisedOn(),
                            value.memo(),
                            value.createdAt(),
                            value.updatedAt()
                    );
            case LifeLogJournalResult.MediaSource value ->
                    new PlayerLifeLogJournalResponse.MediaSource(
                            value.category(),
                            value.title(),
                            value.originalTitle(),
                            value.currentEpisode(),
                            value.totalEpisode(),
                            value.status(),
                            value.rating(),
                            value.tags(),
                            value.rewatchCount(),
                            value.startedOn(),
                            value.finishedOn(),
                            value.createdAt(),
                            value.updatedAt()
                    );
        };
    }
}
