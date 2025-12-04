package online.lifeasgame.character.api.player.response;

import java.util.List;

public final class AchievementResponse {

    private AchievementResponse() {
    }

    public record Infos(
            List<Info> infos
    ) {
    }

    public record Info(
            String code,
            String name,
            String category,
            String descMd
    ) {
    }
}
