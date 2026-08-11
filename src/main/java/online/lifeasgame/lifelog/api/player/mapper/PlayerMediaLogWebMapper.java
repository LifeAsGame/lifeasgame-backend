package online.lifeasgame.lifelog.api.player.mapper;

import online.lifeasgame.lifelog.api.player.request.PlayerMediaLogRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerMediaLogResponse;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.query.MediaLogQuery;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.result.MediaLogResult;

import java.util.List;

public final class PlayerMediaLogWebMapper {

    private PlayerMediaLogWebMapper() {
    }

    public static MediaLogQuery.Search toSearchQuery(
            String category,
            String status,
            String titleLike,
            int page,
            int size
    ) {
        return new MediaLogQuery.Search(category, status, titleLike, page, size);
    }

    public static List<PlayerMediaLogResponse.Info> toInfos(List<MediaLogResult.Info> results) {
        return results.stream().map(PlayerMediaLogWebMapper::toInfo).toList();
    }

    public static MediaLogCommand.Create toCreateCommand(PlayerMediaLogRequest.Create request) {
        return new MediaLogCommand.Create(
                request.category(),
                request.title(),
                request.originalTitle(),
                request.currentEpisode(),
                request.totalEpisode(),
                request.status(),
                request.tags(),
                new LifeLogRecordMetadataCommand(
                        request.lifeLogSubtype(),
                        request.reflectionScope(),
                        request.primaryRoleId(),
                        request.roleEventId()
                )
        );
    }

    public static PlayerMediaLogResponse.Created toCreated(MediaLogResult.Created result) {
        return new PlayerMediaLogResponse.Created(result.id());
    }

    public static MediaLogCommand.Rate toRateCommand(PlayerMediaLogRequest.Rate request) {
        return new MediaLogCommand.Rate(request.score());
    }

    public static MediaLogCommand.Advance toAdvanceCommand(PlayerMediaLogRequest.Advance request) {
        return new MediaLogCommand.Advance(request.step());
    }

    public static MediaLogCommand.MarkStatus toMarkStatusCommand(PlayerMediaLogRequest.MarkStatus request) {
        return new MediaLogCommand.MarkStatus(request.status());
    }

    public static PlayerMediaLogResponse.Info toInfo(MediaLogResult.Info result) {
        return new PlayerMediaLogResponse.Info(
                result.id(),
                result.playerId(),
                result.category(),
                result.title(),
                result.originalTitle(),
                result.currentEpisode(),
                result.totalEpisode(),
                result.status(),
                result.rating(),
                result.tags(),
                result.rewatchCount(),
                result.startedOn(),
                result.finishedOn(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static MediaLogCommand.Update toUpdateCommand(PlayerMediaLogRequest.Update request) {
        return new MediaLogCommand.Update(
                request.category(),
                request.title(),
                request.originalTitle(),
                request.currentEpisode(),
                request.totalEpisode(),
                request.status(),
                request.tags()
        );
    }

    public static PlayerMediaLogResponse.Deleted toDeleted(MediaLogResult.Deleted result) {
        return new PlayerMediaLogResponse.Deleted(result.mediaId());
    }
}
