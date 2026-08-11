package online.lifeasgame.lifelog.api.player.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public final class PlayerExerciseRequest {

    private PlayerExerciseRequest(){}

    public record Create(
            @NotBlank String category,
            @NotNull @Min(1) Integer durationMinutes,
            @DecimalMin("0.0") Double distanceKm,
            @Min(0) Integer calories,
            @NotNull LocalDate exercisedOn,
            String memo,
            String lifeLogSubtype,
            String reflectionScope,
            @Positive Long primaryRoleId,
            @Positive Long roleEventId
    ) {
        public Create(
                String category,
                Integer durationMinutes,
                Double distanceKm,
                Integer calories,
                LocalDate exercisedOn,
                String memo,
                String lifeLogSubtype,
                String reflectionScope
        ) {
            this(
                    category,
                    durationMinutes,
                    distanceKm,
                    calories,
                    exercisedOn,
                    memo,
                    lifeLogSubtype,
                    reflectionScope,
                    null,
                    null
            );
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
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    public record Update(
            String category,
            @Min(1) Integer durationMinutes,
            @DecimalMin("0.0") Double distanceKm,
            @Min(0) Integer calories,
            LocalDate exercisedOn,
            String memo
    ) {}

    public record Search(
            String category,
            LocalDate from,
            LocalDate to,
            @Min(0) int page,
            @Min(1) int size
    ) {}
}
