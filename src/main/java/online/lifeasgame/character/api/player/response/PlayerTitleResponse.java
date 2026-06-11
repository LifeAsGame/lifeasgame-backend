package online.lifeasgame.character.api.player.response;

import java.time.Instant;
import java.util.List;

public final class PlayerTitleResponse {

    private PlayerTitleResponse() {
    }

    public record Infos(List<Info> infos) {
    }

    public record Info(
            Long titleId,
            String code,
            String name,
            String category,
            String descMd,
            Instant acquiredAt
    ) {
    }
}
