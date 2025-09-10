package online.lifeasgame.character.application.command;


public class PlayerCommand {

    public record Register(String name, String gender) {
        public static Register of(String name, String gender) {
            return new Register(name, gender);
        }
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
}
