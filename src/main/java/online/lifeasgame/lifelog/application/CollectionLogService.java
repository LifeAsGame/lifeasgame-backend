package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import online.lifeasgame.lifelog.domain.*;
import online.lifeasgame.lifelog.domain.event.CollectionLogged;
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
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public CollectionResult.Created create(Long playerId, CollectionCommand.Create command) {
        Title title = Title.of(command.title(), command.originalTitle());

        CollectionLog saved = collectionLogWriter.create(
                CollectionLog.create(
                    playerId,
                    CollectionCategory.parse(command.category()),
                    title,
                    Quantity.of(command.quantity()),
                    command.conditionNote(),
                    command.acquiredFrom(),
                    CollectionTags.of(command.tags())
                )
        );

        domainEventPublisher.publish(
                CollectionLogged.of(
                        playerId,
                        saved.getId(),
                        saved.getCategory().name(),
                        saved.getQuantity().value()
                )
        );

        return new CollectionResult.Created(saved.getId());
    }

    @Transactional
    public CollectionResult.Info update(Long playerId, Long collectionId, CollectionCommand.Update command) {
        CollectionLog collectionLog = collectionLogReader.getByIdAndPlayerIdOrThrow(collectionId, playerId);

        if (command.quantity() != null) {
            collectionLog.changeQuantity(command.quantity());
        }
        if (command.conditionNote() != null) {
            collectionLog.changeCondition(command.conditionNote());
        }
        if (command.acquiredFrom() != null) {
            collectionLog.changeAcquiredFrom(command.acquiredFrom());
        }

        return CollectionResult.Info.from(collectionLog);
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

    public CollectionResult.Info getCollection(Long playerId, Long collectionId) {
        CollectionLog collectionLog = collectionLogReader.getByIdAndPlayerIdOrThrow(collectionId, playerId);
        return CollectionResult.Info.from(collectionLog);
    }

    public CollectionResult.Deleted delete(Long playerId, Long collectionId) {
        collectionLogWriter.delete(playerId, collectionId);
        return new CollectionResult.Deleted(playerId, collectionId);
    }
}
