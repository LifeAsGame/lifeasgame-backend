package online.lifeasgame.lifelog.api.player.response;

import java.time.Instant;
import java.time.LocalDate;

public final class PlayerExerciseResponse {
    private PlayerExerciseResponse() {
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
