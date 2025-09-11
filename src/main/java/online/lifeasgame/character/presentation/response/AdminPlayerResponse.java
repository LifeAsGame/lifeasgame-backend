package online.lifeasgame.character.presentation.response;

import java.util.List;

public class AdminPlayerResponse {

    private AdminPlayerResponse() {
    }

    public record ExpGranted(
            Long playerId,
            long requestedExp,
            long appliedExp,
            long leftoverExp,
            int level,
            long totalExp,
            long currentExp,
            long restExp,
            long capacityToNextLevel,
            double progressRatio
    ) {
        public static AdminPlayerResponse.ExpGranted of(
                Long playerId,
                long requestedExp,
                long appliedExp,
                long leftoverExp,
                int level,
                long totalExp,
                long currentExp,
                long restExp,
                long capacityToNextLevel,
                double progressRatio
        ) {
            return new AdminPlayerResponse.ExpGranted(
                    playerId,
                    requestedExp,
                    appliedExp,
                    leftoverExp,
                    level,
                    totalExp,
                    currentExp,
                    restExp,
                    capacityToNextLevel,
                    progressRatio
            );
        }
    }

    public record CoreStatsGranted(
            Long playerId, int str, int agi, int dex, int intel, int vit, int luc
    ) {
        public static CoreStatsGranted of(Long playerId, int str, int agi, int dex, int intel, int vit, int luc) {
            return new CoreStatsGranted(playerId, str, agi, dex, intel, vit, luc);
        }
    }

    public record StatusEffectsGranted(
            Long playerId,
            List<Item> effects
    ) {
        public static StatusEffectsGranted of(Long playerId, List<Item> effects) {
            return new StatusEffectsGranted(playerId, effects);
        }

        public record Item(String code, String category) {
        }
    }
}
