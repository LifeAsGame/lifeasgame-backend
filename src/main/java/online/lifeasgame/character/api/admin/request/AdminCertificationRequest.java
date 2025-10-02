package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;

public class AdminCertificationRequest {

    private AdminCertificationRequest() {
    }

    public record Create(
            @NotBlank String name,
            @NotBlank String issuer,
            @NotBlank String category
    ) {
        public static Create of(String name, String issuer, String category) {
            return new Create(name, issuer, category);
        }
    }
}
