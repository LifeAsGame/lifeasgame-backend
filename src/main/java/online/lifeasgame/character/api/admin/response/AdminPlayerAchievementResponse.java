package online.lifeasgame.character.api.admin.response;

import java.time.Instant;

public final class AdminPlayerAchievementResponse {

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
    }
}
