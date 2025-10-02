package online.lifeasgame.character.api.admin.response;

import java.time.Instant;

public class AdminPlayerAchievementResponse {

    private AdminPlayerAchievementResponse() {
    }

    public record Granted(
            Long playerId,
            Long achievementId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
        public static Granted of(
                Long playerId,
                Long achievementId,
                String code,
                String name,
                String category,
                Instant acquiredAt
        ) {
            return new Granted(
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
