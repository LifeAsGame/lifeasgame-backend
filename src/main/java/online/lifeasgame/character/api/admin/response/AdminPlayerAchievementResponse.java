package online.lifeasgame.character.api.admin.response;

import java.time.Instant;

public class AdminPlayerAchievementResponse {

    private AdminPlayerAchievementResponse() {
    }

    public record GrantedAchievement(
            Long playerId,
            Long achievementId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
        public static AdminPlayerAchievementResponse.GrantedAchievement of(
                Long playerId,
                Long achievementId,
                String code,
                String name,
                String category,
                Instant acquiredAt
        ) {
            return new AdminPlayerAchievementResponse.GrantedAchievement(
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
