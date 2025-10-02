package online.lifeasgame.character.api.admin.response;

import java.time.Instant;
import java.time.LocalDate;

public class AdminPlayerCertificationResponse {

    private AdminPlayerCertificationResponse() {
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
        public static AdminPlayerCertificationResponse.GrantedCertification of(
                Long playerId,
                Long certificationId,
                String name,
                String issuer,
                String category,
                LocalDate acquiredDate,
                LocalDate expiresDate,
                Instant grantedAt
        ) {
            return new AdminPlayerCertificationResponse.GrantedCertification(
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
