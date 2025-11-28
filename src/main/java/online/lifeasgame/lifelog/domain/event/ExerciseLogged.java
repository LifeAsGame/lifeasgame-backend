package online.lifeasgame.lifelog.domain.event;

import online.lifeasgame.core.event.DomainEvent;

import java.time.Instant;

public record ExerciseLogged(
        Long playerId,
        Long exerciseLogId,
        String category,
        int durationMinutes,
        Double distanceKm,
        Integer calories,
        Instant occurredAt
) implements DomainEvent {
    public ExerciseLogged {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static ExerciseLogged of(
            Long playerId,
            Long exerciseLogId,
            String category,
            int durationMinutes,
            Double distanceKm,
            Integer calories
    ) {
        return new ExerciseLogged(
                playerId,
                exerciseLogId,
                category,
                durationMinutes,
                distanceKm,
                calories,
                Instant.now()
        );
    }
}
