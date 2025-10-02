package online.lifeasgame.character.api.admin.response;

public final class AdminCertificationResponse {

    private AdminCertificationResponse() {
    }

    public record Info(
            String name,
            String issuer,
            String category
    ) {
        public static Info of(String name, String issuer, String category) {
            return new Info(name, issuer, category);
        }
    }
}
