package online.lifeasgame.character.presentation.response;

import java.util.List;

public class AchievementResponse {

    private AchievementResponse() {
    }

    public record AchievementInfos(
            List<AchievementResponse.AchievementInfo> achievementInfos
    ) {
        public static AchievementResponse.AchievementInfos of(List<AchievementResponse.AchievementInfo> achievementInfos) {
            return new AchievementResponse.AchievementInfos(achievementInfos);
        }
    }

    public record AchievementInfo(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static AchievementResponse.AchievementInfo of(String code, String name, String category, String descMd) {
            return new AchievementResponse.AchievementInfo(code, name, category, descMd);
        }
    }
}
