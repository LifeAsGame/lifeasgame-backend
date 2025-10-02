package online.lifeasgame.character.api.admin.response;

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
        public static ExpGranted of(
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
            return new ExpGranted(
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

    public record CurrentHp(
            int currentHp
    ) {
        public static CurrentHp of(int currentHp) {
            return new CurrentHp(currentHp);
        }
    }

    public record HpCapacity(
            int hpCapacity
    ) {
        public static HpCapacity of(int hpCapacity) {
            return new HpCapacity(hpCapacity);
        }
    }

    public record CurrentMp(
            int currentMp
    ) {
        public static CurrentMp of(int currentMp) {
            return new CurrentMp(currentMp);
        }
    }

    public record MpCapacity(
            int mpCapacity
    ) {
        public static MpCapacity of(int mpCapacity) {
            return new MpCapacity(mpCapacity);
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

    public record UpdatedTitle(Long titleId) {
        public static UpdatedTitle of(Long titleId) {
            return new UpdatedTitle(titleId);
        }
    }
}
