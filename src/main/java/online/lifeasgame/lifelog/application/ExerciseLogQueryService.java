package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.lifelog.application.query.ExerciseQuery;
import online.lifeasgame.lifelog.application.result.ExerciseResult;
import online.lifeasgame.lifelog.domain.ExerciseLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseLogQueryService {

    private final ExerciseLogReader exerciseLogReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public List<ExerciseResult.Info> recent(int limit) {
        return recent(currentPlayerAccessor.currentPlayerIdOrThrow(), limit);
    }

    public List<ExerciseResult.Info> recent(Long playerId, int limit) {
        return exerciseLogReader.recent(playerId, limit).stream()
                .map(ExerciseResult.Info::from)
                .toList();
    }

    public List<ExerciseResult.Info> search(ExerciseQuery.Search query) {
        return search(currentPlayerAccessor.currentPlayerIdOrThrow(), query);
    }

    public List<ExerciseResult.Info> search(Long playerId, ExerciseQuery.Search query) {
        return exerciseLogReader.search(
                        playerId,
                        query.category(),
                        query.from(),
                        query.to(),
                        query.page(),
                        query.size()
                ).stream()
                .map(ExerciseResult.Info::from)
                .toList();
    }

    public ExerciseResult.Info getExercise(Long exerciseId) {
        return getExercise(currentPlayerAccessor.currentPlayerIdOrThrow(), exerciseId);
    }

    public ExerciseResult.Info getExercise(Long playerId, Long exerciseId) {
        ExerciseLog exerciseLog = exerciseLogReader
                .getByIdAndPlayerIdOrThrow(exerciseId, playerId);
        return ExerciseResult.Info.from(exerciseLog);
    }
}
