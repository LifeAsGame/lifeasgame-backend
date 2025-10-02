package online.lifeasgame.character.application.command;


import java.util.List;

public final class PlayerCommand {

    public record Register(String name, String gender) {
        public static Register of(String name, String gender) {
            return new Register(name, gender);
        }
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
        public static ChangeHp of(Long playerId, int hpDelta) {
            return new ChangeHp(playerId, hpDelta);
        }
    }

    public record ChangeHpCapacity(
            Long playerId,
            int hpCapacityDelta
    ) {
        public static ChangeHpCapacity of(Long playerId, int hpCapacityDelta) {
            return new ChangeHpCapacity(playerId, hpCapacityDelta);
        }
    }

    public record ChangeMp(
            Long playerId,
            int mpDelta
    ) {
        public static ChangeMp of(Long playerId, int mpDelta) {
            return new ChangeMp(playerId, mpDelta);
        }
    }

    public record ChangeMpCapacity(
            Long playerId,
            int mpCapacityDelta
    ) {
        public static ChangeMpCapacity of(Long playerId, int mpCapacityDelta) {
            return new ChangeMpCapacity(playerId, mpCapacityDelta);
        }
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
