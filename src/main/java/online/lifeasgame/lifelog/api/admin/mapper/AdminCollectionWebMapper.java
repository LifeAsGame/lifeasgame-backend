package online.lifeasgame.lifelog.api.admin.mapper;

import online.lifeasgame.lifelog.api.admin.request.AdminCollectionRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminCollectionResponse;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.result.CollectionResult;

import java.util.List;

public final class AdminCollectionWebMapper {
    private AdminCollectionWebMapper() {
    }

    public static CollectionCommand.Create toCommand(AdminCollectionRequest.Create request) {
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

    public static CollectionCommand.Update toCommand(AdminCollectionRequest.Update request) {
        return new CollectionCommand.Update(request.quantity(), request.conditionNote(), request.acquiredFrom());
    }

    public static CollectionCommand.Search toCommand(AdminCollectionRequest.Search request) {
        return new CollectionCommand.Search(request.category(), request.titleLike(), request.page(), request.size());
    }

    public static AdminCollectionResponse.Created toResponse(CollectionResult.Created result) {
        return new AdminCollectionResponse.Created(result.id());
    }

    public static AdminCollectionResponse.Info toResponse(CollectionResult.Info result) {
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

    public static List<AdminCollectionResponse.Info> toResponseList(List<CollectionResult.Info> results) {
        return results.stream().map(AdminCollectionWebMapper::toResponse).toList();
    }

    public static CollectionCommand.Search toCommand(String category, String titleLike, int page, int size) {
        return new CollectionCommand.Search(category, titleLike, page, size);
    }
}
