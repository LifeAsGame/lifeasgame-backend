package online.lifeasgame.character.api.admin.response;

import java.util.List;

public final class AdminAchievementResponse {

    private AdminAchievementResponse() {
    }

    public record Info(
            Long achievementId,
            String code,
            String name,
            String category,
            String description
    ) {
    }

    public record Deleted(Long achievementId) {}

    public record Infos(List<Info> infos) {}
}
