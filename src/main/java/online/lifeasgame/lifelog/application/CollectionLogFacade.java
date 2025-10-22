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

    private final CollectionLogService service;
    private final CurrentPlayerAccessor current;

    public CollectionResult.Created create(CollectionCommand.Create command) {
        Long playerId = current.currentPlayerIdOrThrow();
        return service.create(playerId, command);
    }

    public CollectionResult.Info update(Long collectionId, CollectionCommand.Update command) {
        Long playerId = current.currentPlayerIdOrThrow();
        return service.update(playerId, collectionId, command);
    }

    public List<CollectionResult.Info> recent(int limit) {
        Long playerId = current.currentPlayerIdOrThrow();
        return service.recent(playerId, limit);
    }

    public List<CollectionResult.Info> search(CollectionCommand.Search command) {
        Long playerId = current.currentPlayerIdOrThrow();
        return service.search(playerId, command);
    }
}
