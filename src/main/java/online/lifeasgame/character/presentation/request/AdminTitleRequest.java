package online.lifeasgame.character.presentation.request;

public class AdminTitleRequest {

    private AdminTitleRequest() {
    }

    public record CreateTitle(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static CreateTitle of(String code, String name, String category, String descMd) {
            return new CreateTitle(code, name, category, descMd);
        }
    }
}
