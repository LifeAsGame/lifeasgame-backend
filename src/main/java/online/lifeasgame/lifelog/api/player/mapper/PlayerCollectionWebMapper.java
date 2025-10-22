package online.lifeasgame.lifelog.api.player.mapper;

import online.lifeasgame.lifelog.api.player.request.PlayerCollectionRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerCollectionResponse;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.result.CollectionResult;

import java.util.List;

public final class PlayerCollectionWebMapper {
    private PlayerCollectionWebMapper() {
    }

    public static CollectionCommand.Create toCommand(PlayerCollectionRequest.Create request) {
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

    public static CollectionCommand.Update toCommand(PlayerCollectionRequest.Update request) {
        return new CollectionCommand.Update(request.quantity(), request.conditionNote(), request.acquiredFrom());
    }

    public static CollectionCommand.Search toCommand(PlayerCollectionRequest.Search request) {
        return new CollectionCommand.Search(request.category(), request.titleLike(), request.page(), request.size());
    }

    public static PlayerCollectionResponse.Created toResponse(CollectionResult.Created result) {
        return new PlayerCollectionResponse.Created(result.id());
    }

    public static PlayerCollectionResponse.Info toResponse(CollectionResult.Info result) {
        return new PlayerCollectionResponse.Info(
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

    public static List<PlayerCollectionResponse.Info> toResponseList(List<CollectionResult.Info> results) {
        return results.stream().map(PlayerCollectionWebMapper::toResponse).toList();
    }

    public static CollectionCommand.Search toCommand(String category, String titleLike, int page, int size) {
        return new CollectionCommand.Search(category, titleLike, page, size);
    }
}
