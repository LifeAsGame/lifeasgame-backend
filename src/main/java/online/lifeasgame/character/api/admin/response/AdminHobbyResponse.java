package online.lifeasgame.character.api.admin.response;

public final class AdminHobbyResponse {

    private AdminHobbyResponse() {
    }

    public record Info(
            Long hobbyId,
            String name,
            String category
    ) {
    }
}
