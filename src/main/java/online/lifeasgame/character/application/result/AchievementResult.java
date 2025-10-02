package online.lifeasgame.character.application.result;

import online.lifeasgame.character.domain.Achievement;

import java.util.List;

public class AchievementResult {

    private AchievementResult() {
    }

    public record Info(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static Info from(Achievement achievement) {
            return new Info(
                    achievement.getCode(),
                    achievement.getName(),
                    achievement.getCategory().name(),
                    achievement.getDescMd()
            );
        }

        public static List<Info> fromList(List<Achievement> achievements) {
            return achievements.stream().map(Info::from).toList();
        }

        public static AchievementResult.Info of(String code, String name, String category, String descMd) {
            return new AchievementResult.Info(code, name, category, descMd);
        }
    }
}
