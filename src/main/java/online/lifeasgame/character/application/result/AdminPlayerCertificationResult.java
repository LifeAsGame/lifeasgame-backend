package online.lifeasgame.character.application.result;

import java.time.Instant;
import java.time.LocalDate;

public class AdminPlayerCertificationResult {

    private AdminPlayerCertificationResult() {
    }

    public record GrantedCertification(
            Long playerId,
            Long certificationId,
            String name,
            String issuer,
            String category,
            LocalDate acquiredDate,
            LocalDate expiresDate,
            Instant grantedAt
    ) {
        public static AdminPlayerCertificationResult.GrantedCertification of(
                Long playerId,
                Long certificationId,
                String name,
                String issuer,
                String category,
                LocalDate acquiredDate,
                LocalDate expiresDate,
                Instant grantedAt
        ) {
            return new AdminPlayerCertificationResult.GrantedCertification(
                    playerId,
                    certificationId,
                    name,
                    issuer,
                    category,
                    acquiredDate,
                    expiresDate,
                    grantedAt
            );
        }
    }
}
