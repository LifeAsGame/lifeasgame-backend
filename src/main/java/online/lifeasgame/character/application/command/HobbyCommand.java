package online.lifeasgame.character.application.command;


public final class HobbyCommand {

    private HobbyCommand() {
    }

    public record Create(
            String name,
            String category
    ) {
    }
}
