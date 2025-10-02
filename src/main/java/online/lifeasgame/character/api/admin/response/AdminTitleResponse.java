package online.lifeasgame.character.api.admin.response;

public class AdminTitleResponse {

    private AdminTitleResponse() {
    }

    public record TitleInfo(
            String code,
            String name,
            String category,
            String description
    ) {
        public static TitleInfo of(String code, String name, String category, String description) {
            return new TitleInfo(code, name, category, description);
        }
    }
}
