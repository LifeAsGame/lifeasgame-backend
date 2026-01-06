package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;

public final class AdminAchievementRequest {

    private AdminAchievementRequest() {}

    public record Create(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String category,
            @NotBlank String descMd
    ) {
    }

    public record Update(
            String code,
            String name,
            String category,
            String descMd
    ) {}
}
