package online.lifeasgame.character.presentation.response;

import java.util.List;
import java.util.Map;

public class PlayerResponse {

    private PlayerResponse() {
    }

    public record Created(Long id) {
        public static Created of(Long id) {
            return new Created(id);
        }
    }

    public record PlayerInfo(
            String name,
            String gender,
            String job,
            int level,
            long exp,
            int currentHealth,
            int healthCapacity,
            int currentMana,
            int manaCapacity,
            int str, int agi, int dex, int intel, int vit, int luc,
            Map<String, Integer> extraStats,
            List<String> effects
    ) {
        public static PlayerInfo of(
                String name,
                String gender,
                String job,
                int level,
                long exp,
                int currentHealth,
                int healthCapacity,
                int currentMana,
                int manaCapacity,
                int str, int agi, int dex, int intel, int vit, int luc,
                Map<String, Integer> stringIntegerMap,
                List<String> effects
        ) {
            return new PlayerInfo(
                    name,
                    gender,
                    job,
                    level,
                    exp,
                    currentHealth,
                    healthCapacity,
                    currentMana,
                    manaCapacity,
                    str, agi, dex, intel, vit, luc,
                    stringIntegerMap,
                    effects
            );
        }
    }

    public record CurrentHp(
            int currentHp
    ) {
        public static CurrentHp of(int currentHp) {
            return new CurrentHp(currentHp);
        }
    }

    public record HpCapacity(
            int hpCapacity
    ) {
        public static HpCapacity of(int hpCapacity) {
            return new HpCapacity(hpCapacity);
        }
    }

    public record CurrentMp(
            int currentMp
    ) {
        public static CurrentMp of(int currentMp) {
            return new CurrentMp(currentMp);
        }
    }

    public record MpCapacity(
            int mpCapacity
    ) {
        public static MpCapacity of(int mpCapacity) {
            return new MpCapacity(mpCapacity);
        }
    }
}
