package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;

public class AdminAchievementRequest {

    private AdminAchievementRequest() {
    }

    public record CreateAchievement(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String category,
            @NotBlank String descMd
    ) {
        public static CreateAchievement of(String code, String name, String category, String descMd) {
            return new CreateAchievement(code, name, category, descMd);
        }
    }
}
