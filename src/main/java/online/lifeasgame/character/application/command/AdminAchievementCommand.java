package online.lifeasgame.character.application.command;


public class AdminAchievementCommand {

    private AdminAchievementCommand() {
    }

    public record CreateAchievement(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static CreateAchievement of(String code, String name, String category, String descMd) {
            return new CreateAchievement(code, name, category, descMd);
        }
    }
}
