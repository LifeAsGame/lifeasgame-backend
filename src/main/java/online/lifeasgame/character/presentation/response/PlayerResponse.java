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
            List<StatusEffects> effects
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
                List<StatusEffects> effects
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

        public record StatusEffects(String code, String effect) {
            public static StatusEffects of(String code, String effect) {
                return new StatusEffects(code, effect);
            }
        }
    }
}
