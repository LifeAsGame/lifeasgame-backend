package online.lifeasgame.lifelog.api.player.mapper;

import online.lifeasgame.lifelog.api.player.request.PlayerMediaLogRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerMediaLogResponse;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.result.MediaLogResult;

import java.util.List;

public final class PlayerMediaLogWebMapper {
    private PlayerMediaLogWebMapper() {
    }

    public static MediaLogCommand.Create toCommand(PlayerMediaLogRequest.Create request) {
        return new MediaLogCommand.Create(
                request.category(),
                request.title(),
                request.originalTitle(),
                request.currentEpisode(),
                request.totalEpisode(),
                request.status(),
                request.tags()
        );
    }

    public static MediaLogCommand.Rate toCommand(PlayerMediaLogRequest.Rate request) {
        return new MediaLogCommand.Rate(request.score());
    }

    public static MediaLogCommand.Advance toCommand(PlayerMediaLogRequest.Advance request) {
        return new MediaLogCommand.Advance(request.step());
    }

    public static MediaLogCommand.MarkStatus toCommand(PlayerMediaLogRequest.MarkStatus request) {
        return new MediaLogCommand.MarkStatus(request.status());
    }

    public static MediaLogCommand.Search toCommand(
            String category,
            String status,
            String titleLike,
            int page,
            int size
    ) {
        return new MediaLogCommand.Search(
                category,
                status,
                titleLike,
                page,
                size
        );
    }

    public static PlayerMediaLogResponse.Created toResponse(MediaLogResult.Created result) {
        return new PlayerMediaLogResponse.Created(result.id());
    }

    public static PlayerMediaLogResponse.Info toResponse(MediaLogResult.Info result) {
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

    public static List<PlayerMediaLogResponse.Info> toResponseList(List<MediaLogResult.Info> results) {
        return results.stream().map(PlayerMediaLogWebMapper::toResponse).toList();
    }
}
