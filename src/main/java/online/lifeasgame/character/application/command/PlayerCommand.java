package online.lifeasgame.character.application.command;


import java.util.List;

public final class PlayerCommand {

    public record Register(String name, String gender) {
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

    public record ChangeHp(
            Long playerId,
            int hpDelta
    ) {
    }

    public record ChangeHpCapacity(
            Long playerId,
            int hpCapacityDelta
    ) {
    }

    public record ChangeMp(
            Long playerId,
            int mpDelta
    ) {
    }

    public record ChangeMpCapacity(
            Long playerId,
            int mpCapacityDelta
    ) {
    }

    public record GrantStatusEffects(
            Long playerId,
            List<String> codes
    ) {
    }

    public record Renamed(String name) {
    }
}
