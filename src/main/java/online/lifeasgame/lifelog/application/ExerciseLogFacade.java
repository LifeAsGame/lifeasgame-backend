package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExerciseLogFacade {

    private final ExerciseLogService exerciseLogService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public ExerciseResult.Created create(ExerciseCommand.Create command) {
        Long playerId = getPlayer();
        return exerciseLogService.create(playerId, command);
    }

    public ExerciseResult.Info update(Long exerciseId, ExerciseCommand.Update command) {
        Long playerId = getPlayer();
        return exerciseLogService.update(playerId, exerciseId, command);
    }

    public List<ExerciseResult.Info> recent(int limit) {
        Long playerId = getPlayer();
        return exerciseLogService.recent(playerId, limit);
    }

    public List<ExerciseResult.Info> search(ExerciseCommand.Search command) {
        Long playerId = getPlayer();
        return exerciseLogService.search(playerId, command);
    }

    private Long getPlayer() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }

    public ExerciseResult.Info getExercise(Long exerciseId) {
        Long playerId = getPlayer();
        return exerciseLogService.getExercise(playerId, exerciseId);
    }
}
