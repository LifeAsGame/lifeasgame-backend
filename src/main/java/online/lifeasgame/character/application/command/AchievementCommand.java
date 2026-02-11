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
    }

    public record Update(
            String code,
            String name,
            String category,
            String descMd
    ) {
    }
}
