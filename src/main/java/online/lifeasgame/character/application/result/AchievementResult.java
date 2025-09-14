package online.lifeasgame.character.application.result;

import java.util.List;
import online.lifeasgame.character.domain.Achievement;

public class AchievementResult {

    private AchievementResult() {
    }

    public record AchievementInfo(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static AchievementInfo from(Achievement achievement) {
            return new AchievementInfo(
                    achievement.getCode(),
                    achievement.getName(),
                    achievement.getCategory().name(),
                    achievement.getDescMd()
            );
        }

        public static List<AchievementInfo> fromList(List<Achievement> achievements) {
            return achievements.stream().map(AchievementResult.AchievementInfo::from).toList();
        }
    }
}
