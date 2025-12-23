package online.lifeasgame.character.api.admin.response;

public final class AdminTitleResponse {

    private AdminTitleResponse() {
    }

    public record Info(
            String code,
            String name,
            String category,
            String description
    ) {
    }
}
