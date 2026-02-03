package online.lifeasgame.user.application.command;

public final class UserCommand {

    private UserCommand() {
    }

    public record Register(String email, String password, String nickname) {
    }

    public record ChangePassword(
            String currentPassword,
            String newPassword
    ) {
    }

    public record Search(
            String email,
            String nickname,
            String status,
            int page,
            int size
    ) {
    }

    public record ChangeStatus(
            String status,
            String reason
    ) {
    }
}
