package online.lifeasgame.character.api.admin.response;

public final class AdminCertificationResponse {

    private AdminCertificationResponse() {
    }

    public record Info(
            String name,
            String issuer,
            String category
    ) {
    }
}
