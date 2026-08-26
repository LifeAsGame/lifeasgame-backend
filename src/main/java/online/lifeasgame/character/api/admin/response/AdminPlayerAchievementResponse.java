package online.lifeasgame.character.api.admin.response;

import java.time.Instant;
import java.util.List;

public final class AdminPlayerAchievementResponse {

    private AdminPlayerAchievementResponse() {
    }

    public record Infos(Long playerId, List<Info> infos) {
    }

    public record Info(
            Long achievementId,
            String code,
            String name,
            String category,
            Instant acquiredAt
    ) {
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

    public record Revoked(Long playerId, Long achievementId) {}
}
