package online.lifeasgame.character.application.result;

public class AdminAchievementResult {

    private AdminAchievementResult() {
    }

    public record AchievementInfo(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static AchievementInfo of(String code, String name, String category, String descMd) {
            return new AchievementInfo(code, name, category, descMd);
        }
    }
}
