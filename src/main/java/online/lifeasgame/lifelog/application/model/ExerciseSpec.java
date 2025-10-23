package online.lifeasgame.lifelog.application.model;

import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.domain.ExerciseCategory;
import online.lifeasgame.lifelog.domain.ExerciseMetrics;

public final class ExerciseSpec {
    private ExerciseSpec() {
    }

    public record Create(
            Long playerId,
            ExerciseCategory category,
            ExerciseMetrics metrics,
            java.time.LocalDate exercisedOn,
            String memo
    ) {
        public static Create from(Long playerId, ExerciseCommand.Create command) {
            return new Create(
                    playerId,
                    ExerciseCategory.parse(command.category()),
                    ExerciseMetrics.of(command.durationMinutes(), command.distanceKm(), command.calories()),
                    command.exercisedOn(),
                    command.memo()
            );
        }
    }
}
