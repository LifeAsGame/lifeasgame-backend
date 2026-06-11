package online.lifeasgame.character.api.player.response;

import java.time.Instant;
import java.util.List;

public final class PlayerAchievementResponse {

    private PlayerAchievementResponse() {
    }

    public record Infos(List<Info> infos) {
    }

    public record Info(
            Long achievementId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
    }
}
