package online.lifeasgame.character.application.command;

public class AdminPlayerCommand {

    private AdminPlayerCommand() {
    }


    public record GrantCoreStats(
            Long playerId,
            int str,
            int agi,
            int dex,
            int intel,
            int vit,
            int luc
    ) {
    }
}
