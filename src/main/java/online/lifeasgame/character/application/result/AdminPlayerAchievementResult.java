package online.lifeasgame.character.application.result;

import java.time.Instant;

public class AdminPlayerAchievementResult {

    private AdminPlayerAchievementResult() {
    }

    public record GrantedAchievement(
            Long playerId,
            Long achievementId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
        public static AdminPlayerAchievementResult.GrantedAchievement of(
                Long playerId,
                Long achievementId,
                String code,
                String name,
                String category,
                Instant acquiredAt
        ) {
            return new AdminPlayerAchievementResult.GrantedAchievement(
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
