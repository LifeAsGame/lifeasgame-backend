package online.lifeasgame.character.api.player.response;

import java.util.List;
import java.util.Map;

public final class PlayerResponse {

    private PlayerResponse() {
    }

    public record Created(Long id) {
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

        public record StatusEffects(String code, String effect) {
        }
    }

    public record Updated(Long titleId) {
    }
}
