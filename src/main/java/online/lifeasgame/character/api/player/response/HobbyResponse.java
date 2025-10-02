package online.lifeasgame.character.api.player.response;

import java.util.List;

public class HobbyResponse {

    private HobbyResponse() {
    }

    public record HobbyInfos(
            List<HobbyResponse.HobbyInfo> hobbyInfos
    ) {
        public static HobbyResponse.HobbyInfos of(List<HobbyResponse.HobbyInfo> hobbyInfos) {
            return new HobbyResponse.HobbyInfos(hobbyInfos);
        }
    }

    public record HobbyInfo(
            Long hobbyId,
            String name,
            String category
    ) {
        public static HobbyResponse.HobbyInfo of(Long hobbyId, String name, String category) {
            return new HobbyResponse.HobbyInfo(hobbyId, name, category);
        }
    }
}
