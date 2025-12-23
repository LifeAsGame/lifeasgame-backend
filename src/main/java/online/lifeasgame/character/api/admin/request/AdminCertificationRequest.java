package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;

public final class AdminCertificationRequest {

    private AdminCertificationRequest() {}

    public record Create(
            @NotBlank String name,
            @NotBlank String issuer,
            @NotBlank String category
    ) {
    }
}
