package online.lifeasgame.character.api.admin.response;

import java.util.List;

public final class AdminHobbyResponse {

    private AdminHobbyResponse() {
    }

    public record Info(
            Long hobbyId,
            String name,
            String category
    ) {
    }

    public record Deleted(Long hobbyId) {}

    public record Infos(List<Info> infos) {
    }
}
