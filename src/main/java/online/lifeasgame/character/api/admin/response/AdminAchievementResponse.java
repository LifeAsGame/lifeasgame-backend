package online.lifeasgame.character.api.admin.response;

public final class AdminAchievementResponse {

    private AdminAchievementResponse() {
    }

    public record Info(
            String code,
            String name,
            String category,
            String description
    ) {
    }
}
