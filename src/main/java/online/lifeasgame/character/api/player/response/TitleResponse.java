package online.lifeasgame.character.api.player.response;

import java.util.List;

public final class TitleResponse {

    private TitleResponse() {
    }

    public record Infos(
            List<Info> infos
    ) {
    }

    public record Info(
            Long titleId,
            String code,
            String name,
            String category,
            String descMd
    ) {
    }
}
