package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CollectionLogFacade {

    private final CollectionLogService collectionLogService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public CollectionResult.Created create(CollectionCommand.Create command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return collectionLogService.create(playerId, command);
    }

    public CollectionResult.Info update(Long collectionId, CollectionCommand.Update command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return collectionLogService.update(playerId, collectionId, command);
    }

    public List<CollectionResult.Info> recent(int limit) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return collectionLogService.recent(playerId, limit);
    }

    public List<CollectionResult.Info> search(CollectionCommand.Search command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return collectionLogService.search(playerId, command);
    }

    public CollectionResult.Info getCollection(Long collectionId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return collectionLogService.getCollection(playerId, collectionId);
    }

    public CollectionResult.Deleted delete(Long collectionId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return collectionLogService.delete(playerId, collectionId);
    }
}
