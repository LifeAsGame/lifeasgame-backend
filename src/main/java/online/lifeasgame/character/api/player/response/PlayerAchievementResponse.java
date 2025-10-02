package online.lifeasgame.character.api.player.response;

import java.time.Instant;
import java.util.List;

public class PlayerAchievementResponse {

    private PlayerAchievementResponse() {
    }

    public record Infos(List<Info> infos) {
        public static Infos of(List<Info> infos) {
            return new Infos(infos);
        }
    }

    public record Info(
            Long achievementId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
        public static Info of(
                Long achievementId,
                String code,
                String name,
                String category,
                String descMd,
                Instant acquiredAt
        ) {
            return new Info(achievementId, code, name, category, descMd, acquiredAt);
        }
    }
}
