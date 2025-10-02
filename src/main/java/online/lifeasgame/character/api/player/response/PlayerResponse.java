package online.lifeasgame.character.api.player.response;

import java.util.List;
import java.util.Map;

public final class PlayerResponse {

    private PlayerResponse() {
    }

    public record Created(Long id) {
        public static Created of(Long id) {
            return new Created(id);
        }
    }

    public record Info(
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
        public static Info of(
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
            return new Info(
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

    public record Updated(Long titleId) {
        public static Updated of(Long titleId) {
            return new Updated(titleId);
        }
    }
}
