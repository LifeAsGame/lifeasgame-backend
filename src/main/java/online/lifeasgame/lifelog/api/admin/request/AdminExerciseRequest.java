package online.lifeasgame.lifelog.api.admin.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public final class AdminExerciseRequest {
    private AdminExerciseRequest() {
    }

    public record Create(
            @NotBlank String category,
            @NotNull @Min(1) Integer durationMinutes,
            @DecimalMin("0.0") Double distanceKm,
            @Min(0) Integer calories,
            @NotNull LocalDate exercisedOn,
            String memo
    ) {
    }

    public record Update(
            String category,
            @Min(1) Integer durationMinutes,
            @DecimalMin("0.0") Double distanceKm,
            @Min(0) Integer calories,
            LocalDate exercisedOn,
            String memo
    ) {
    }

    public record Search(String category, LocalDate from, LocalDate to, @Min(0) int page, @Min(1) int size) {
    }
}
