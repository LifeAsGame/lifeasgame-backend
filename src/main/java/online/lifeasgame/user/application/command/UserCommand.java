package online.lifeasgame.user.application.command;

public class UserCommand {
    public record Register(String email, String password, String nickname) {
    }
}
