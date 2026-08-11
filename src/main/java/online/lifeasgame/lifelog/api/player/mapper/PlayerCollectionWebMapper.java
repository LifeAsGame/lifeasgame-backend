package online.lifeasgame.lifelog.api.player.mapper;

import online.lifeasgame.lifelog.api.player.request.PlayerCollectionRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerCollectionResponse;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.query.CollectionQuery;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.result.CollectionResult;

import java.util.List;

public final class PlayerCollectionWebMapper {

    private PlayerCollectionWebMapper() {
    }

    public static CollectionQuery.Search toSearchQuery(
            String category,
            String titleLike,
            int page,
            int size
    ) {
        return new CollectionQuery.Search(category, titleLike, page, size);
    }

    public static List<PlayerCollectionResponse.Info> toInfos(List<CollectionResult.Info> results) {
        return results.stream().map(PlayerCollectionWebMapper::toInfo).toList();
    }

    public static CollectionCommand.Create toCreateCommand(PlayerCollectionRequest.Create request) {
        return new CollectionCommand.Create(
                request.category(),
                request.title(),
                request.originalTitle(),
                request.quantity(),
                request.conditionNote(),
                request.acquiredFrom(),
                request.tags(),
                new LifeLogRecordMetadataCommand(
                        request.lifeLogSubtype(),
                        request.reflectionScope(),
                        request.primaryRoleId(),
                        request.roleEventId()
                )
        );
    }

    public static PlayerCollectionResponse.Created toCreated(CollectionResult.Created result) {
        return new PlayerCollectionResponse.Created(result.id());
    }

    public static CollectionCommand.Update toUpdateCommand(PlayerCollectionRequest.Update request) {
        return new CollectionCommand.Update(request.quantity(), request.conditionNote(), request.acquiredFrom());
    }

    public static PlayerCollectionResponse.Info toInfo(CollectionResult.Info result) {
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

    public static PlayerCollectionResponse.Deleted toDeleted(CollectionResult.Deleted result) {
        return new PlayerCollectionResponse.Deleted(
                result.collectionId()
        );
    }
}
