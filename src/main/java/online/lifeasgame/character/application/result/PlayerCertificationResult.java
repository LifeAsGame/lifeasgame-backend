package online.lifeasgame.character.application.result;

import java.time.Instant;
import java.time.LocalDate;
import online.lifeasgame.character.application.view.PlayerCertificationView;

public class PlayerCertificationResult {

    private PlayerCertificationResult() {
    }

    public record PlayerCertificationInfo(
            Long certificationId,
            String name,
            String issuer,
            String category,
            LocalDate acquiredDate,
            LocalDate expiresDate,
            Instant grantedAt
    ) {
        public static PlayerCertificationInfo from(PlayerCertificationView v) {
            return new PlayerCertificationInfo(
                    v.getCertificationId(),
                    v.getName(),
                    v.getIssuer(),
                    v.getCategory() != null ? v.getCategory().name() : null,
                    v.getAcquiredDate(),
                    v.getExpiresDate(),
                    v.getGrantedAt()
            );
        }
    }
}
