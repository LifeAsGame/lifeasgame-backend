package online.lifeasgame.character.application.result;

import java.time.Instant;
import online.lifeasgame.character.application.view.PlayerAchievementView;

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
}
