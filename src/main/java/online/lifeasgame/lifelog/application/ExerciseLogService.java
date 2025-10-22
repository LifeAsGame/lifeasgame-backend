package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.model.ExerciseSpec;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import online.lifeasgame.lifelog.domain.ExerciseMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ExerciseLogService {

    private final ExerciseLogReader exerciseLogReader;
    private final ExerciseLogWriter exerciseLogWriter;

    @Transactional
    public ExerciseResult.Created create(Long playerId, ExerciseCommand.Create command) {
        ExerciseLog saved = exerciseLogWriter.create(ExerciseSpec.Create.from(playerId, command));
        return ExerciseResult.Created.of(saved.getId());
    }

    @Transactional
    public ExerciseResult.Info update(Long playerId, Long exerciseId, ExerciseCommand.Update command) {
        ExerciseLog exerciseLog = exerciseLogReader.get(exerciseId, playerId);
        exerciseLogWriter.update(
                exerciseLog, new ExerciseSpec.Create(
                        playerId,
                        ExerciseCategory.parse(command.category() == null ? exerciseLog.getCategory().name() : command.category()),
                        ExerciseMetrics.of(
                                command.durationMinutes() == null ? exerciseLog.getMetrics().durationMinutes() : command.durationMinutes(),
                                command.distanceKm() == null ? exerciseLog.getMetrics().distanceKm() : command.distanceKm(),
                                command.calories() == null ? exerciseLog.getMetrics().calories() : command.calories()
                        ),
                        command.exercisedOn() == null ? exerciseLog.getExercisedOn() : command.exercisedOn(),
                        command.memo() == null ? exerciseLog.getMemo() : command.memo()
                )
        );

        return ExerciseResult.Info.from(exerciseLog);
    }

    public List<ExerciseResult.Info> recent(Long playerId, int limit) {
        return exerciseLogReader.recent(playerId, limit).stream().map(ExerciseResult.Info::from).toList();
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
}
