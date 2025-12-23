package online.lifeasgame.character.api.player.response;

import java.util.List;

public final class CertificationResponse {

    private CertificationResponse() {
    }

    public record Infos(
            List<Info> infos
    ) {
    }

    public record Info(
            Long certificationId,
            String name,
            String issuer,
            String category
    ) {
    }
}
