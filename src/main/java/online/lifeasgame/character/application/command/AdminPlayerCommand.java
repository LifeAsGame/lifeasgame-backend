package online.lifeasgame.character.application.command;

public class AdminPlayerCommand {

    private AdminPlayerCommand() {
    }

    public record GrantCoreStats(
            Long playerId,
            int strDelta,
            int agiDelta,
            int dexDelta,
            int intelDelta,
            int vitDelta,
            int lucDelta
    ) {
    }
}
