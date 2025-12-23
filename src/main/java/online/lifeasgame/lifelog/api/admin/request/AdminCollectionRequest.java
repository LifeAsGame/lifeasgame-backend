package online.lifeasgame.lifelog.api.admin.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public final class AdminCollectionRequest {

    private AdminCollectionRequest() {
    }

    public record Search(
            String category,
            String titleLike,
            @Min(0) int page,
            @Min(1) int size
    ) {
    }

    public record Create(
            @NotBlank String category,
            @NotBlank String title,
            String originalTitle,
            @NotNull @Min(1) Integer quantity,
            String conditionNote,
            String acquiredFrom,
            Set<String> tags
    ) {
    }

    public record Update(
            @Min(1) Integer quantity,
            String conditionNote,
            String acquiredFrom
    ) {
    }
}
