package online.lifeasgame.character.application.result;

public class AdminTitleResult {

    private AdminTitleResult() {
    }

    public record TitleInfo(
            String code,
            String name,
            String category,
            String descMd
    ) {
        public static TitleInfo of(String code, String name, String category, String descMd) {
            return new TitleInfo(code, name, category, descMd);
        }
    }
}
