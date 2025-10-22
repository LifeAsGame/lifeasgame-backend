package online.lifeasgame.lifelog.domain.repository;

import online.lifeasgame.lifelog.domain.ExerciseLog;

import java.util.Optional;

public interface ExerciseLogRepository {
    ExerciseLog save(ExerciseLog log);
    Optional<ExerciseLog> findById(Long id);
    Optional<ExerciseLog> findByIdAndPlayerId(Long id, Long playerId);
}