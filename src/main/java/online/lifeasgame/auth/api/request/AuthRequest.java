package online.lifeasgame.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequest {

    private AuthRequest() {
    }

    public record Login(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record Refresh(
            @NotBlank String refreshToken
    ) {}

    public record Register(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 2, max = 20) String nickname
    ) {
    }
}
