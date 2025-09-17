package online.lifeasgame.character.application.command;


public class AdminCertificationCommand {

    private AdminCertificationCommand() {
    }

    public record CreateCertification(
            String name,
            String issuer,
            String category
    ) {
        public static CreateCertification of(String name, String issuer, String category) {
            return new CreateCertification(name, issuer, category);
        }
    }
}
