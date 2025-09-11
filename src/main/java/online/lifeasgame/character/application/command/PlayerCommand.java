package online.lifeasgame.character.application.command;


public class PlayerCommand {

    public record Register(String name, String gender) {
        public static Register of(String name, String gender) {
            return new Register(name, gender);
        }
    }
}
