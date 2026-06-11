package online.lifeasgame.lifelog.api.admin.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AdminExerciseResponse {

    private AdminExerciseResponse() {
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

    public record Infos(List<Info> items) {
    }

    public record Created(Long id) {
    }

    public record Deleted(Long id) {
    }

    public record Page<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
