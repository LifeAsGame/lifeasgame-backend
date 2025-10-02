package online.lifeasgame.character.api.player.response;

import java.time.Instant;
import java.util.List;

public class PlayerTitleResponse {

    private PlayerTitleResponse() {
    }

    public record Infos(List<Info> infos) {
        public static Infos of(List<Info> infos) {
            return new Infos(infos);
        }
    }

    public record Info(
            Long titleId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
        public static Info of(
                Long titleId,
                String code,
                String name,
                String category,
                String descMd,
                Instant acquiredAt
        ) {
            return new Info(titleId, code, name, category, descMd, acquiredAt);
        }
    }
}
