package online.lifeasgame.character.api.admin.response;

public class AdminCertificationResponse {

    private AdminCertificationResponse() {
    }

    public record CertificationInfo(
            String name,
            String issuer,
            String category
    ) {
        public static CertificationInfo of(String name, String issuer, String category) {
            return new CertificationInfo(name, issuer, category);
        }
    }
}
