package online.lifeasgame.user.api.user.request;

import jakarta.validation.constraints.*;

public final class UserRequest {

    private UserRequest() {
    }

    public record Register(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 2, max = 20) String nickname
    ) {
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

    public record UpdateSettings(
            @Min(0) @Max(100) Integer volume,
            String uiLayoutJson,
            String flagsJson
    ) {
    }

}
