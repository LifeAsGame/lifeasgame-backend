package online.lifeasgame.user.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserCommand {

    private UserCommand() {}

    public record Register(String email, String password, String nickname) {
    }

    public record ChangePassword(
            @NotBlank @Size(min = 8, max = 72) String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {
    }
}
