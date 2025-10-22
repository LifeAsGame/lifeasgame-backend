package online.lifeasgame.lifelog.application.query;

import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseLogQueryRepository {
    List<ExerciseLog> findByPlayer(Long playerId, int limit);

    List<ExerciseLog> search(
            Long playerId,
            ExerciseCategory category,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    );
}
