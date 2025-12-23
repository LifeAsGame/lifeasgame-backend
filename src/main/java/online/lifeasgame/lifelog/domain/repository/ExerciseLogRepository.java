package online.lifeasgame.lifelog.domain.repository;

import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExerciseLogRepository {
    ExerciseLog save(ExerciseLog log);

    Optional<ExerciseLog> findById(Long id);

    Optional<ExerciseLog> findByIdAndPlayerId(Long id, Long playerId);

    List<ExerciseLog> findByPlayerId(Long playerId, int limit);

    List<ExerciseLog> search(
            Long playerId,
            ExerciseCategory category,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    );
}