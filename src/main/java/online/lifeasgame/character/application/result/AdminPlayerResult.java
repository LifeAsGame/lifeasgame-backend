package online.lifeasgame.character.application.result;

import java.util.List;
import online.lifeasgame.character.domain.CoreStats;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.character.domain.Player.GainResult;
import online.lifeasgame.character.domain.StatusEffects;

public class AdminPlayerResult {

    private AdminPlayerResult() {
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
        public static ExpGranted of(Long id, GainResult gainResult) {
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


    public record CurrentHp(
            int value
    ) {
        public static CurrentHp from(Player player) {
            return new CurrentHp(player.getHealth().current());
        }
    }

    public record HpCapacity(
            int cap
    ) {
        public static HpCapacity from(Player player) {
            return new HpCapacity(player.getHealth().cap());
        }
    }

    public record CurrentMp(
            int value
    ) {
        public static CurrentMp from(Player player) {
            return new CurrentMp(player.getMana().current());
        }
    }

    public record MpCapacity(
            int cap
    ) {
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
        public static CoreStatsGranted of(Long playerId, CoreStats coreStats) {
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
            List<Item> effects
    ) {
        public static StatusEffectsGranted from(Long playerId, StatusEffects statusEffects) {
            return new StatusEffectsGranted(
                    playerId,
                    statusEffects.asList().stream()
                            .map(code -> new Item(code.name(), code.category().name()))
                            .toList()
            );
        }

        public record Item(String code, String category) {
        }
    }

    public record UpdatedTitle(Long titleId) {
        public static UpdatedTitle of(Long titleId) {
            return new UpdatedTitle(titleId);
        }
    }
}
