package online.lifeasgame.lifelog.api.player.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public final class PlayerCollectionRequest {

    private PlayerCollectionRequest() {
    }

    public record Create(
            @NotBlank String category,
            @NotBlank String title,
            String originalTitle,
            @NotNull @Min(1) Integer quantity,
            String conditionNote,
            String acquiredFrom,
            Set<String> tags,
            String lifeLogSubtype,
            String reflectionScope
    ) {
        public Create(
                String category,
                String title,
                String originalTitle,
                Integer quantity,
                String conditionNote,
                String acquiredFrom,
                Set<String> tags
        ) {
            this(
                    category,
                    title,
                    originalTitle,
                    quantity,
                    conditionNote,
                    acquiredFrom,
                    tags,
                    null,
                    null
            );
        }
    }

    public record Update(
            @Min(1) Integer quantity,
            String conditionNote,
            String acquiredFrom
    ) {
    }

    public record Search(
            String category,
            String titleLike,
            @Min(0) int page,
            @Min(1) int size
    ) {
    }
}
