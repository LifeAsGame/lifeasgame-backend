package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.support.IdGenerator;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;
import online.lifeasgame.lifelog.application.record.LifeLogRecordService;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.ExerciseMetrics;
import online.lifeasgame.lifelog.domain.event.ExerciseLogged;
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
public class ExerciseLogService {

    private final ExerciseLogReader exerciseLogReader;
    private final ExerciseLogWriter exerciseLogWriter;
    private final LifeLogRecordService lifeLogRecordService;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public ExerciseResult.Created create(Long playerId, ExerciseCommand.Create command) {
        return create(
                playerId,
                command,
                LifeLogEntryMode.FULL,
                command.lifeLogMetadata()
        );
    }

    @Transactional
    public ExerciseResult.Created createQuick(
            Long playerId,
            ExerciseCommand.Create command,
            LifeLogRecordMetadataCommand metadata
    ) {
        return create(playerId, command, LifeLogEntryMode.QUICK, metadata);
    }

    private ExerciseResult.Created create(
            Long playerId,
            ExerciseCommand.Create command,
            LifeLogEntryMode entryMode,
            LifeLogRecordMetadataCommand metadata
    ) {
        ExerciseLog saved = exerciseLogWriter.create(
                ExerciseLog.create(
                        playerId,
                        ExerciseCategory.parse(command.category()),
                        ExerciseMetrics.of(command.durationMinutes(), command.distanceKm(), command.calories()),
                        command.exercisedOn(),
                        command.memo()
                )
        );

        LifeLogRecord record = lifeLogRecordService.create(
                playerId,
                LifeLogSourceType.EXERCISE,
                saved.getId(),
                entryMode,
                metadata
        );
        Instant occurredAt = record.getOccurredAt();
        domainEventPublisher.publishAll(List.of(
                new ExerciseLogged(
                        playerId,
                        saved.getId(),
                        saved.getCategory().name(),
                        saved.getMetrics().durationMinutes(),
                        saved.getMetrics().distanceKm(),
                        saved.getMetrics().calories(),
                        occurredAt
                ),
                LifeLogRecorded.from(
                        IdGenerator.newEventId(),
                        record
                )
        ));

        return new ExerciseResult.Created(
                saved.getId(),
                record.getId(),
                occurredAt
        );
    }

    @Transactional
    public ExerciseResult.Info update(Long playerId, Long exerciseId, ExerciseCommand.Update command) {
        ExerciseLog exerciseLog = exerciseLogReader.getByIdAndPlayerIdOrThrow(exerciseId, playerId);

        exerciseLog.changeMetrics(
                ExerciseMetrics.of(
                        command.durationMinutes(),
                        command.distanceKm(),
                        command.calories()
                )
        );
        exerciseLog.changeExercisedOn(command.exercisedOn());
        exerciseLog.changeMemo(command.memo());

        return ExerciseResult.Info.from(exerciseLog);
    }

    public List<ExerciseResult.Info> recent(Long playerId, int limit) {
        return exerciseLogReader.recent(playerId, limit).stream()
                .map(ExerciseResult.Info::from)
                .toList();
    }

    public List<ExerciseResult.Info> search(Long playerId, ExerciseCommand.Search command) {
        return exerciseLogReader.search(
                playerId,
                command.category(),
                command.from(),
                command.to(),
                command.page(),
                command.size()
        ).stream()
                .map(ExerciseResult.Info::from)
                .toList();
    }

    public ExerciseResult.Info getExercise(Long playerId, Long exerciseId) {
        ExerciseLog exerciseLog = exerciseLogReader.getByIdAndPlayerIdOrThrow(exerciseId, playerId);
        return ExerciseResult.Info.from(exerciseLog);
    }

    @Transactional
    public ExerciseResult.Deleted delete(Long playerId, Long exerciseId) {
        exerciseLogWriter.delete(playerId, exerciseId);
        return new ExerciseResult.Deleted(exerciseId);
    }
}
