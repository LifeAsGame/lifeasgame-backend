package online.lifeasgame.character.application.command;

public class AdminTitleCommand {

    private AdminTitleCommand() {
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
