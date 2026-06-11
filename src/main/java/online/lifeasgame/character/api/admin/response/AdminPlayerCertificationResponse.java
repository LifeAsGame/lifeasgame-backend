package online.lifeasgame.character.api.admin.response;

import java.time.Instant;
import java.time.LocalDate;

public final class AdminPlayerCertificationResponse {

    private AdminPlayerCertificationResponse() {
    }

    public record Granted(
            Long playerId,
            Long certificationId,
            String name,
            String issuer,
            String category,
            LocalDate acquiredDate,
            LocalDate expiresDate,
            Instant grantedAt
    ) {
    }

    public record Revoked(Long playerId, Long certificationId) {}
}
