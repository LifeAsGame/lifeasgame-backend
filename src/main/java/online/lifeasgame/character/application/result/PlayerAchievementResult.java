package online.lifeasgame.character.application.result;

import online.lifeasgame.character.application.view.PlayerAchievementView;

import java.time.Instant;

public class PlayerAchievementResult {

    private PlayerAchievementResult() {
    }

    public record PlayerAchievementInfo(
            Long achievementId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
        public static PlayerAchievementInfo from(PlayerAchievementView v) {
            return new PlayerAchievementInfo(
                    v.getAchievementId(),
                    v.getCode(),
                    v.getName(),
                    v.getCategory() != null ? v.getCategory().name() : null,
                    v.getDescMd(),
                    v.getAcquiredAt()
            );
        }
    }

    public record GrantedAchievement(
            Long playerId,
            Long achievementId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
        public static GrantedAchievement of(
                Long playerId,
                Long achievementId,
                String code,
                String name,
                String category,
                Instant acquiredAt
        ) {
            return new GrantedAchievement(
                    playerId,
                    achievementId,
                    code,
                    name,
                    category,
                    acquiredAt
            );
        }
    }
}
