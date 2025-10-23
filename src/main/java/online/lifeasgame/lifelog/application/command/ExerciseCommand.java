package online.lifeasgame.lifelog.application.command;

import java.time.LocalDate;

public final class ExerciseCommand {
    private ExerciseCommand() {
    }

    public record Create(
            String category,
            Integer durationMinutes,
            Double distanceKm,
            Integer calories,
            LocalDate exercisedOn,
            String memo
    ) {
    }

    public record Update(
            String category,
            Integer durationMinutes,
            Double distanceKm,
            Integer calories,
            LocalDate exercisedOn,
            String memo
    ) {
    }

    public record Search(
            String category,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
    }
}
