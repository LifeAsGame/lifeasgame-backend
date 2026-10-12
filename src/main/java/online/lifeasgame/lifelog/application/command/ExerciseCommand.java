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
            LifeLogRecordMetadataCommand lifeLogMetadata,
            Long personalCategoryId
    ) {
        public Create(String category, Integer durationMinutes, Double distanceKm, Integer calories,
                      LocalDate exercisedOn, String memo, LifeLogRecordMetadataCommand lifeLogMetadata) {
            this(category, durationMinutes, distanceKm, calories, exercisedOn, memo, lifeLogMetadata, null);
        }
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
                    LifeLogRecordMetadataCommand.none(),
                    null
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

}
