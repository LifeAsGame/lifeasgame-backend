package online.lifeasgame.character.application.command;


public class AdminHobbyCommand {

    private AdminHobbyCommand() {
    }

    public record CreateHobby(
            String name,
            String category
    ) {
        public static CreateHobby of(String name, String category) {
            return new CreateHobby(name, category);
        }
    }
}
