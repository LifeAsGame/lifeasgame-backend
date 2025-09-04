package online.lifeasgame.character.application.result;

import java.util.List;
import java.util.Map;
import online.lifeasgame.character.domain.Player;

public class PlayerResult {

    private PlayerResult() {
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
        public static PlayerInfo from(Player player) {
            return new PlayerInfo(
                    player.getName().value(),
                    player.getGender().name(),
                    player.getJob(),
                    player.getLevel().value(),
                    player.getExp().value(),
                    player.getHealth().current(),
                    player.getHealth().cap(),
                    player.getMana().current(),
                    player.getMana().cap(),
                    player.getStats().str(),
                    player.getStats().agi(),
                    player.getStats().dex(),
                    player.getStats().intel(),
                    player.getStats().vit(),
                    player.getStats().luc(),
                    player.getExtraStats().asMap(),
                    player.getStatusEffects().asList()
            );
        }
    }
}
