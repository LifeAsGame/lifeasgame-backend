package online.lifeasgame.character.api.player.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PlayerCertificationResponse {

    private PlayerCertificationResponse() {
    }

    public record Infos(List<Info> infos) {
    }

    public record Info(
            Long certificationId,
            String name,
            String issuer,
            String category,
            LocalDate acquiredDate,
            LocalDate expiresDate,
            Instant grantedAt
    ) {
    }

    public record Changed(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
    }

    public record Created(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
    }
}
