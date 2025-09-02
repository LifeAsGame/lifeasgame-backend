package online.lifeasgame.user.application.command;

public class UserCommand {
    public record Register(String email, String password, String nickname) {
        public static Register of(
                String email,
                String password,
                String nickname
        ) {
            return new Register(email, password, nickname);
        }
    }
}
