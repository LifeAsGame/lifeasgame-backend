package online.lifeasgame.lifelog.application.command;

import online.lifeasgame.lifelog.application.record.LifeLogRecordMetadataCommand;

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
            String memo,
            LifeLogRecordMetadataCommand lifeLogMetadata
    ) {
        public Create(
                String category,
                Integer durationMinutes,
                Double distanceKm,
                Integer calories,
                LocalDate exercisedOn,
                String memo
        ) {
            this(
                    category,
                    durationMinutes,
                    distanceKm,
                    calories,
                    exercisedOn,
                    memo,
                    LifeLogRecordMetadataCommand.none()
            );
        }
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
