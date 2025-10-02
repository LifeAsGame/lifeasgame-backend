package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;

public class AdminCertificationRequest {

    private AdminCertificationRequest() {
    }

    public record CreateCertification(
            @NotBlank String name,
            @NotBlank String issuer,
            @NotBlank String category
    ) {
        public static CreateCertification of(String name, String issuer, String category) {
            return new CreateCertification(name, issuer, category);
        }
    }
}
