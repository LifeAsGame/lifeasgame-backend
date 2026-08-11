package online.lifeasgame.lifelog.api.player.request;

import jakarta.validation.constraints.*;

import java.util.Set;

public final class PlayerMediaLogRequest {

    private PlayerMediaLogRequest() {
    }

    public record Create(
            @NotBlank String category,
            @NotBlank String title,
            String originalTitle,
            @Min(0) Integer currentEpisode,
            @Min(1) Integer totalEpisode,
            @NotBlank String status,
            Set<String> tags,
            String lifeLogSubtype,
            String reflectionScope,
            @Positive Long primaryRoleId,
            @Positive Long roleEventId
    ) {
        public Create(
                String category,
                String title,
                String originalTitle,
                Integer currentEpisode,
                Integer totalEpisode,
                String status,
                Set<String> tags,
                String lifeLogSubtype,
                String reflectionScope
        ) {
            this(
                    category,
                    title,
                    originalTitle,
                    currentEpisode,
                    totalEpisode,
                    status,
                    tags,
                    lifeLogSubtype,
                    reflectionScope,
                    null,
                    null
            );
        }

        public Create(
                String category,
                String title,
                String originalTitle,
                Integer currentEpisode,
                Integer totalEpisode,
                String status,
                Set<String> tags
        ) {
            this(
                    category,
                    title,
                    originalTitle,
                    currentEpisode,
                    totalEpisode,
                    status,
                    tags,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    public record Update(
            String category,
            String title,
            String originalTitle,
            @Min(0) Integer currentEpisode,
            @Min(1) Integer totalEpisode,
            String status,
            Set<String> tags
    ) {}

    public record Rate(
            @DecimalMin("0.0") @DecimalMax("5.0") Double score,
            String idempotencyKey
    ) {
    }

    public record Advance(
            @Min(1) Integer step,
            String idempotencyKey
    ) {
    }

    public record MarkStatus(
            @NotBlank String status,
            String idempotencyKey
    ) {
    }
}
