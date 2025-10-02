package online.lifeasgame.character.api.response;

import java.time.Instant;
import java.util.List;

public class PlayerAchievementResponse {

    private PlayerAchievementResponse() {
    }

    public record PlayerAchievementInfos(List<PlayerAchievementInfo> playerAchievementInfos) {
        public static PlayerAchievementInfos of(List<PlayerAchievementInfo> playerAchievementInfos) {
            return new PlayerAchievementInfos(playerAchievementInfos);
        }
    }

    public record PlayerAchievementInfo(
            Long achievementId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
        public static PlayerAchievementInfo of(
                Long achievementId,
                String code,
                String name,
                String category,
                String descMd,
                Instant acquiredAt
        ) {
            return new PlayerAchievementInfo(achievementId, code, name, category, descMd, acquiredAt);
        }
    }
}
