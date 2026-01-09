package online.lifeasgame.user.application.command;

public final class UserCommand {

    private UserCommand() {}

    public record Register(String email, String password, String nickname) {
    }

    public record ChangePassword(
            String currentPassword,
            String newPassword
    ) {
    }
}
