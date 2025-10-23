package online.lifeasgame.lifelog.api.admin.response;

import java.time.Instant;
import java.time.LocalDate;

public final class AdminExerciseResponse {
    private AdminExerciseResponse() {
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
    }
}
