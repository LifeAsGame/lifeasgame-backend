package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.support.IdGenerator;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.ExerciseMetrics;
import online.lifeasgame.lifelog.domain.event.ExerciseLogged;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExerciseLogService {

    private final ExerciseLogReader exerciseLogReader;
    private final ExerciseLogWriter exerciseLogWriter;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @Transactional
    public ExerciseResult.Created create(Long playerId, ExerciseCommand.Create command) {
        ExerciseLog saved = exerciseLogWriter.create(
                ExerciseLog.create(
                        playerId,
                        ExerciseCategory.parse(command.category()),
                        ExerciseMetrics.of(command.durationMinutes(), command.distanceKm(), command.calories()),
                        command.exercisedOn(),
                        command.memo()
                )
        );

        Instant occurredAt = clock.instant();
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
                LifeLogRecorded.of(
                        IdGenerator.newEventId(),
                        playerId,
                        saved.getId(),
                        LifeLogType.EXERCISE,
                        occurredAt
                )
        ));

        return new ExerciseResult.Created(saved.getId());
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
