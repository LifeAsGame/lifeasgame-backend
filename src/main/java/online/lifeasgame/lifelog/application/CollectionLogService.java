package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.model.CollectionSpec;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import online.lifeasgame.lifelog.domain.CollectionLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class CollectionLogService {

    private final CollectionLogReader collectionLogReader;
    private final CollectionLogWriter collectionLogWriter;

    @Transactional
    public CollectionResult.Created create(Long playerId, CollectionCommand.Create command) {
        CollectionLog saved = collectionLogWriter.create(CollectionSpec.Create.from(playerId, command));
        return CollectionResult.Created.of(saved.getId());
    }

    @Transactional
    public CollectionResult.Info update(Long playerId, Long collectionId, CollectionCommand.Update command) {
        CollectionLog log = collectionLogReader.getCollectionLog(collectionId, playerId);

        if (command.quantity() != null) {
            collectionLogWriter.changeQuantity(log, command.quantity());
        }
        if (command.conditionNote() != null) {
            collectionLogWriter.changeCondition(log, command.conditionNote());
        }
        if (command.acquiredFrom() != null) {
            collectionLogWriter.changeAcquiredFrom(log, command.acquiredFrom());
        }

        return CollectionResult.Info.from(log);
    }

    public List<CollectionResult.Info> recent(Long playerId, int limit) {
        return collectionLogReader.recent(playerId, limit).stream()
                .map(CollectionResult.Info::from)
                .toList();
    }

    public List<CollectionResult.Info> search(Long playerId, CollectionCommand.Search command) {
        return collectionLogReader.search(
                        playerId,
                        command.category(),
                        command.titleLike(),
                        command.page(),
                        command.size()
                ).stream()
                .map(CollectionResult.Info::from)
                .toList();
    }
}
