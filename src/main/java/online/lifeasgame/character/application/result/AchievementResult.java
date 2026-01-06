package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.Achievement;

import java.util.List;

public final class AchievementResult {

    private AchievementResult() {
    }

    public record Info(
            Long achievementId,
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static Info from(Achievement achievement) {
            return new Info(
                    achievement.getId(),
                    achievement.getCode(),
                    achievement.getName(),
                    achievement.getCategory().name(),
                    achievement.getDescMd()
            );
        }

        public static List<Info> fromList(List<Achievement> achievements) {
            return achievements.stream().map(Info::from).toList();
        }
    }
}
