package online.lifeasgame.character.application.command;


public final class CertificationCommand {

    private CertificationCommand() {
    }

    public record Create(
            String name,
            String issuer,
            String category
    ) {
        public static Create of(String name, String issuer, String category) {
            return new Create(name, issuer, category);
        }
    }
}
