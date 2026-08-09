package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.core.support.IdGenerator;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordRegistrar;
import online.lifeasgame.lifelog.application.result.CollectionResult;
import online.lifeasgame.lifelog.domain.*;
import online.lifeasgame.lifelog.domain.event.CollectionLogged;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionLogService {

    private final CollectionLogReader collectionLogReader;
    private final CollectionLogWriter collectionLogWriter;
    private final LifeLogRecordRegistrar lifeLogRecordRegistrar;
    private final DomainEventPublisher domainEventPublisher;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public CollectionResult.Created create(CollectionCommand.Create command) {
        return create(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public CollectionResult.Created create(Long playerId, CollectionCommand.Create command) {
        return create(
                playerId,
                command,
                LifeLogEntryMode.FULL,
                command.lifeLogMetadata()
        );
    }

    @Transactional
    public CollectionResult.Created createQuick(
            Long playerId,
            CollectionCommand.Create command,
            LifeLogRecordMetadataCommand metadata
    ) {
        return create(playerId, command, LifeLogEntryMode.QUICK, metadata);
    }

    private CollectionResult.Created create(
            Long playerId,
            CollectionCommand.Create command,
            LifeLogEntryMode entryMode,
            LifeLogRecordMetadataCommand metadata
    ) {
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

        LifeLogRecord record = lifeLogRecordRegistrar.register(
                playerId,
                LifeLogSourceType.COLLECTION,
                saved.getId(),
                entryMode,
                metadata
        );
        Instant occurredAt = record.getOccurredAt();
        domainEventPublisher.publishAll(List.of(
                new CollectionLogged(
                        playerId,
                        saved.getId(),
                        saved.getCategory().name(),
                        saved.getQuantity().value(),
                        occurredAt
                ),
                LifeLogRecorded.from(
                        IdGenerator.newEventId(),
                        record
                )
        ));

        return new CollectionResult.Created(
                saved.getId(),
                record.getId(),
                occurredAt
        );
    }

    @Transactional
    public CollectionResult.Info update(Long collectionId, CollectionCommand.Update command) {
        return update(currentPlayerAccessor.currentPlayerIdOrThrow(), collectionId, command);
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

    @Transactional
    public CollectionResult.Deleted delete(Long collectionId) {
        return delete(currentPlayerAccessor.currentPlayerIdOrThrow(), collectionId);
    }

    @Transactional
    public CollectionResult.Deleted delete(Long playerId, Long collectionId) {
        collectionLogWriter.delete(playerId, collectionId);
        return new CollectionResult.Deleted(playerId, collectionId);
    }
}
