package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.CoreStats;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.StatusEffects;

import java.util.List;
import java.util.Map;

public final class PlayerResult {

    private PlayerResult() {
    }

    public record Created(Long id) {
    }

    public record PlayerInfo(
            Long playerId,
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
            List<StatusEffect> effects,
            Long representativeTitleId
    ) {
        public record StatusEffect(String code, String category) {}

        public static PlayerInfo from(Player player) {
            return new PlayerInfo(
                    player.getId(),
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
                    player.getStatusEffects().asList().stream()
                            .map(
                                    effect -> new StatusEffect(
                                            effect.name(),
                                            effect.category().name()
                                    )
                            )
                            .toList(),
                    player.getTitleId()
            );
        }
    }

    public record UpdatedTitle(Long titleId) {
    }

    public record ExpGranted(
            Long playerId,
            long requestedExp,
            long appliedExp,
            long leftoverExp,
            int level,
            long totalExp,
            long expIntoLevel,
            long expToNext,
            long capForLevel,
            double progressRatio
    ) {
        public static ExpGranted from(Long id, Player.GainResult gainResult) {
            return new ExpGranted(
                    id,
                    gainResult.requestedExp(),
                    gainResult.appliedExp(),
                    gainResult.leftoverExp(),
                    gainResult.afterLevel(),
                    gainResult.totalExp(),
                    gainResult.expIntoLevel(),
                    gainResult.expToNext(),
                    gainResult.capForLevel(),
                    gainResult.progressRatio()
            );
        }
    }


    public record CurrentHp(int value) {
        public static CurrentHp from(Player player) {
            return new CurrentHp(player.getHealth().current());
        }
    }

    public record HpCapacity(int cap) {
        public static HpCapacity from(Player player) {
            return new HpCapacity(player.getHealth().cap());
        }
    }

    public record CurrentMp(int value) {
        public static CurrentMp from(Player player) {
            return new CurrentMp(player.getMana().current());
        }
    }

    public record MpCapacity(int cap) {
        public static MpCapacity from(Player player) {
            return new MpCapacity(player.getMana().cap());
        }
    }

    public record CoreStatsGranted(
            Long playerId,
            int str,
            int agi,
            int dex,
            int intel,
            int vit,
            int luc
    ) {
        public static CoreStatsGranted from(Long playerId, CoreStats coreStats) {
            return new CoreStatsGranted(
                    playerId,
                    coreStats.str(),
                    coreStats.agi(),
                    coreStats.dex(),
                    coreStats.intel(),
                    coreStats.vit(),
                    coreStats.luc()
            );
        }
    }

    public record StatusEffectsGranted(
            Long playerId,
            List<StatusEffectsGranted.Item> effects
    ) {
        public static StatusEffectsGranted from(Long playerId, StatusEffects statusEffects) {
            return new StatusEffectsGranted(
                    playerId,
                    statusEffects.asList().stream()
                            .map(code -> new StatusEffectsGranted.Item(code.name(), code.category().name()))
                            .toList()
            );
        }

        public record Item(String code, String category) {
        }
    }

    public record Players(List<Item> players) {
        public static Players fromList(List<Player> players) {
            return new Players(
                    players.stream()
                            .map(
                                    player -> new Item(
                                            player.getId(),
                                            player.getUserId(),
                                            player.getName().value()
                                    )
                            )
                            .toList()
            );
        }

        public record Item(Long playerId, Long userId, String name) {
        }
    }

    public record Renamed(Long playerId, String name) {

    }
}
