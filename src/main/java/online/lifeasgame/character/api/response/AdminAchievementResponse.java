package online.lifeasgame.character.api.response;

public class AdminAchievementResponse {

    private AdminAchievementResponse() {
    }

    public record AchievementInfo(
            String code,
            String name,
            String category,
            String description
    ) {
        public static AchievementInfo of(String code, String name, String category, String description) {
            return new AchievementInfo(code, name, category, description);
        }
    }
}
