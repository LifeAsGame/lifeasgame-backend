package online.lifeasgame.character.api.player.response;

import java.util.List;

public final class HobbyResponse {

    private HobbyResponse() {
    }

    public record Infos(
            List<Info> infos
    ) {
    }

    public record Info(
            Long hobbyId,
            String name,
            String category
    ) {
    }
}
