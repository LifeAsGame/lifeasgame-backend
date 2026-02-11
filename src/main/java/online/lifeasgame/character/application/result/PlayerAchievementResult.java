package online.lifeasgame.character.application.result;

import online.lifeasgame.character.application.view.PlayerAchievementView;

import java.time.Instant;

public final class PlayerAchievementResult {

    private PlayerAchievementResult() {
    }

    public record Info(
            Long achievementId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
        public static Info from(PlayerAchievementView v) {
            return new Info(
                    v.getAchievementId(),
                    v.getCode(),
                    v.getName(),
                    v.getCategory() != null ? v.getCategory().name() : null,
                    v.getDescMd(),
                    v.getAcquiredAt()
            );
        }
    }

    public record Granted(
            Long playerId,
            Long achievementId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
    }

    public record Revoked(Long playerId, Long achievementId) {
    }
}
