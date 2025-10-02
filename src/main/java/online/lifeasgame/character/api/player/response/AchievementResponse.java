package online.lifeasgame.character.api.player.response;

import java.util.List;

public class AchievementResponse {

    private AchievementResponse() {
    }

    public record Infos(
            List<Info> infos
    ) {
        public static Infos of(List<Info> infos) {
            return new Infos(infos);
        }
    }

    public record Info(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static Info of(String code, String name, String category, String descMd) {
            return new Info(code, name, category, descMd);
        }
    }
}
