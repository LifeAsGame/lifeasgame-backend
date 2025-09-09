package online.lifeasgame.character.application.command;


public class PlayerCommand {

    public record Register(String name, String gender) {
        public static Register of(String name, String gender) {
            return new Register(name, gender);
        }
    }

    public record ChangeHp(
            Long playerId,
            int hp
    ) {
        public static ChangeHp of(Long playerId, int hp) {
            return new ChangeHp(playerId, hp);
        }
    }

    public record ChangeHpCapacity(
            Long playerId,
            int hpCapacity
    ) {
        public static ChangeHpCapacity of(Long playerId, int hpCapacity) {
            return new ChangeHpCapacity(playerId, hpCapacity);
        }
    }

    public record ChangeMp(
            Long playerId,
            int mp
    ) {
        public static ChangeMp of(Long playerId, int mp) {
            return new ChangeMp(playerId, mp);
        }
    }

    public record ChangeMpCapacity(
            Long playerId,
            int mpCapacity
    ) {
        public static ChangeMpCapacity of(Long playerId, int mpCapacity) {
            return new ChangeMpCapacity(playerId, mpCapacity);
        }
    }
}
