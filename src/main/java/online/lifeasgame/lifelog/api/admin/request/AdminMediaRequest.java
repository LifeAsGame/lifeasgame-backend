package online.lifeasgame.lifelog.api.admin.request;

import jakarta.validation.constraints.*;

import java.util.Set;

public final class AdminMediaRequest {

    private AdminMediaRequest() {
    }

    public record Create(
            @NotBlank String category,
            @NotBlank String title,
            String originalTitle,
            @NotNull @Min(0) Integer currentEpisode,
            @NotNull @Min(1) Integer totalEpisode,
            @NotBlank String status,
            Set<String> tags
    ) {
    }

    public record Rate(@NotNull Double score) {
    }

    public record Advance(@NotNull @Min(1) Integer step) {
    }

    public record MarkStatus(@NotBlank String status) {
    }
}
