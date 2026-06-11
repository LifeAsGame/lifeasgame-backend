package online.lifeasgame.user.api.user.request;

import jakarta.validation.constraints.*;

public final class UserRequest {

    private UserRequest() {
    }

    public record ChangeNickname(
            @NotBlank @Size(min = 2, max = 20) String nickname
    ) {
    }

    public record ChangePassword(
            @NotBlank @Size(min = 8, max = 72) String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {
    }

    public record Delete(
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }
}
