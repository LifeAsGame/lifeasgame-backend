package online.lifeasgame.lifelog.application.result;

import online.lifeasgame.lifelog.domain.ExerciseLog;

import java.time.Instant;
import java.time.LocalDate;

public final class ExerciseResult {

    private ExerciseResult() {
    }

    public record Created(Long id) {
    }

    public record Info(
            Long id,
            Long playerId,
            String category,
            Integer durationMinutes,
            Double distanceKm,
            Integer calories,
            LocalDate exercisedOn,
            String memo,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Info from(ExerciseLog log) {
            return new Info(
                    log.getId(),
                    log.getPlayerId(),
                    log.getCategory().name(),
                    log.getMetrics().durationMinutes(),
                    log.getMetrics().distanceKm(),
                    log.getMetrics().calories(),
                    log.getExercisedOn(),
                    log.getMemo(),
                    log.getCreatedAt(),
                    log.getUpdatedAt()
            );
        }
    }

    public record Deleted(Long exerciseId) {
    }
}
