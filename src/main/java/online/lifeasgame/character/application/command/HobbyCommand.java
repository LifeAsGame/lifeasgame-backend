package online.lifeasgame.character.application.command;


public final class HobbyCommand {

    private HobbyCommand() {
    }

    public record Create(
            String name,
            String category
    ) {
        public static Create of(String name, String category) {
            return new Create(name, category);
        }
    }
}
