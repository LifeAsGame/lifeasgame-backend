package online.lifeasgame.character.application.command;

import java.util.List;

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

    public record GrantStatusEffects(
            Long playerId,
            List<String> codes
    ) {
        public static GrantStatusEffects of(Long playerId, List<String> codes) {
            return new GrantStatusEffects(playerId, codes);
        }
    }
}
