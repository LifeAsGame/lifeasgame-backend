package online.lifeasgame.character.application.internal;

import java.time.Instant;
import java.util.List;

public interface AchievementProgressReadApi {

    List<RecentAchievement> recentAchievements(Long playerId, int limit);

    record RecentAchievement(
            Long achievementId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
    }
}
