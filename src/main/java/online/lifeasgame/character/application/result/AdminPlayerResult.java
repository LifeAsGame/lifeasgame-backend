package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.CoreStats;
import online.lifeasgame.character.domain.Player.GainResult;

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
}
