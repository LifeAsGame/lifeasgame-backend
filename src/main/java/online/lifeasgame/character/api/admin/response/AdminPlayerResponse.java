package online.lifeasgame.character.api.admin.response;

import java.util.List;

public final class AdminPlayerResponse {

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
    }

    public record CurrentHp(int currentHp) {
    }

    public record HpCapacity(int hpCapacity) {
    }

    public record CurrentMp(int currentMp) {
    }

    public record MpCapacity(int mpCapacity) {
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
    }

    public record StatusEffectsGranted(
            Long playerId,
            List<Item> effects
    ) {
        public record Item(String code, String category) {
        }
    }

    public record UpdatedTitle(Long titleId) {
    }
}
