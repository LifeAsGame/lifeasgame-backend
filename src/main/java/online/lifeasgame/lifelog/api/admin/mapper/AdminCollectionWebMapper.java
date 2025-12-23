package online.lifeasgame.lifelog.api.admin.mapper;

import online.lifeasgame.lifelog.api.admin.request.AdminCollectionRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminCollectionResponse;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.result.CollectionResult;

import java.util.List;

public final class AdminCollectionWebMapper {

    private AdminCollectionWebMapper() {
    }

    public static CollectionCommand.Search toSearchCommand(String category, String titleLike, int page, int size) {
        return new CollectionCommand.Search(category, titleLike, page, size);
    }

    public static List<AdminCollectionResponse.Info> toInfos(List<CollectionResult.Info> results) {
        return results.stream().map(AdminCollectionWebMapper::toInfo).toList();
    }

    public static CollectionCommand.Create toCreateCommand(AdminCollectionRequest.Create request) {
        return new CollectionCommand.Create(
                request.category(),
                request.title(),
                request.originalTitle(),
                request.quantity(),
                request.conditionNote(),
                request.acquiredFrom(),
                request.tags()
        );
    }

    public static AdminCollectionResponse.Created toCreated(CollectionResult.Created result) {
        return new AdminCollectionResponse.Created(result.id());
    }

    public static CollectionCommand.Update toUpdateCommand(AdminCollectionRequest.Update request) {
        return new CollectionCommand.Update(request.quantity(), request.conditionNote(), request.acquiredFrom());
    }

    public static AdminCollectionResponse.Info toInfo(CollectionResult.Info result) {
        return new AdminCollectionResponse.Info(
                result.id(),
                result.playerId(),
                result.category(),
                result.title(),
                result.originalTitle(),
                result.quantity(),
                result.conditionNote(),
                result.acquiredFrom(),
                result.tags(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
