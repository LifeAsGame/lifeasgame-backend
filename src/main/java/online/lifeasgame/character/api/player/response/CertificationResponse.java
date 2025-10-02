package online.lifeasgame.character.api.player.response;

import java.util.List;

public final class CertificationResponse {

    private CertificationResponse() {
    }

    public record Infos(
            List<Info> infos
    ) {
        public static Infos of(List<Info> infos) {
            return new Infos(infos);
        }
    }

    public record Info(
            Long certificationId,
            String name,
            String issuer,
            String category
    ) {
        public static Info of(Long certificationId, String name, String issuer, String category) {
            return new Info(certificationId, name, issuer, category);
        }
    }
}
