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
        public static Info of(String code, String name, String category, String description) {
            return new Info(code, name, category, description);
        }
    }
}
