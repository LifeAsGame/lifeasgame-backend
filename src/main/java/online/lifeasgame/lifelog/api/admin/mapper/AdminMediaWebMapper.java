package online.lifeasgame.lifelog.api.admin.mapper;

import online.lifeasgame.lifelog.api.admin.request.AdminMediaRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminMediaResponse;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.application.query.MediaLogQuery;
import online.lifeasgame.lifelog.application.result.MediaLogResult;

import java.util.List;

public final class AdminMediaWebMapper {

    private AdminMediaWebMapper() {
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

    public static List<AdminMediaResponse.Info> toInfos(List<MediaLogResult.Info> results) {
        return results.stream().map(AdminMediaWebMapper::toInfo).toList();
    }

    public static MediaLogCommand.Create toCreateCommand(AdminMediaRequest.Create request) {
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

    public static AdminMediaResponse.Created toCreated(MediaLogResult.Created result) {
        return new AdminMediaResponse.Created(result.id());
    }

    public static MediaLogCommand.Rate toRateCommand(AdminMediaRequest.Rate request) {
        return new MediaLogCommand.Rate(request.score());
    }

    public static MediaLogCommand.Advance toAdvanceCommand(AdminMediaRequest.Advance request) {
        return new MediaLogCommand.Advance(request.step());
    }

    public static MediaLogCommand.MarkStatus toMarkStatusCommand(AdminMediaRequest.MarkStatus request) {
        return new MediaLogCommand.MarkStatus(request.status());
    }

    public static AdminMediaResponse.Info toInfo(MediaLogResult.Info result) {
        return new AdminMediaResponse.Info(
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

    public static AdminMediaResponse.Deleted toDeleted(Long mediaId) {
        return new AdminMediaResponse.Deleted(mediaId);
    }
}
