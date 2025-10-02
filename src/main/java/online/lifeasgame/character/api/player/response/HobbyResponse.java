package online.lifeasgame.character.api.player.response;

import java.util.List;

public class HobbyResponse {

    private HobbyResponse() {
    }

    public record Infos(
            List<Info> infos
    ) {
        public static Infos of(List<Info> infos) {
            return new Infos(infos);
        }
    }

    public record Info(
            Long hobbyId,
            String name,
            String category
    ) {
        public static Info of(Long hobbyId, String name, String category) {
            return new Info(hobbyId, name, category);
        }
    }
}
