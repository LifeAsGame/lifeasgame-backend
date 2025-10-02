package online.lifeasgame.character.application.command;


public final class AchievementCommand {

    private AchievementCommand() {
    }

    public record Create(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static Create of(String code, String name, String category, String descMd) {
            return new Create(code, name, category, descMd);
        }
    }
}
