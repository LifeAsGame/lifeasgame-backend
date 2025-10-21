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
            Set<String> tags
    ) {
    }

    public record Rate(@DecimalMin("0.0") @DecimalMax("5.0") Double score) {
    }

    public record Advance(@Min(1) Integer step) {
    }

    public record MarkStatus(@NotBlank String status) {
    }
}
