package online.lifeasgame.character.application.result;

public class AdminCertificationResult {

    private AdminCertificationResult() {
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
