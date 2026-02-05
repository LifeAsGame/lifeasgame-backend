package online.lifeasgame.character.api.admin.response;

import java.util.List;

public final class AdminPlayerResponse {

    private AdminPlayerResponse() {
    }

    public record Players(List<Item> players, PageInfo page) {
        public record Item(Long playerId, Long userId, String name, int level, long totalExp) {
        }

        public record PageInfo(int page, int size, long totalElements) {
        }
    }

    public record PlayerInfo(
            Long playerId,
            String name,
            String gender,
            String job,
            int level,
            long totalExp,
            int currentHealth,
            int healthCapacity,
            int currentMana,
            int manaCapacity,
            int str, int agi, int dex, int intel, int vit, int luc,
            List<StatusEffect> effects,
            Long representativeTitleId
    ) {
        public record StatusEffect(String code, String category) {}
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

    public record StatusEffectsSet(
            Long playerId,
            List<Item> effects
    ) {
        public record Item(String code, String category) {}
    }

    public record UpdatedTitle(Long titleId) {
    }

    public record Renamed(Long playerId, String name) {}
}
