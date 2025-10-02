package online.lifeasgame.character.application.result;

import online.lifeasgame.character.application.view.PlayerCertificationView;
import online.lifeasgame.character.domain.PlayerCertification;

import java.time.Instant;
import java.time.LocalDate;

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

    public record ChangedPlayerCertification(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static ChangedPlayerCertification from(PlayerCertification playerCertification) {
            return new ChangedPlayerCertification(
                    playerCertification.getCertificationId(),
                    playerCertification.getAcquiredDate(),
                    playerCertification.getExpiresDate()
            );
        }
    }

    public record CreatedPlayerCertification(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static CreatedPlayerCertification from(PlayerCertification playerCertification) {
            return new CreatedPlayerCertification(
                    playerCertification.getCertificationId(),
                    playerCertification.getAcquiredDate(),
                    playerCertification.getExpiresDate()
            );
        }
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
        public static GrantedCertification of(
                Long playerId,
                Long certificationId,
                String name,
                String issuer,
                String category,
                LocalDate acquiredDate,
                LocalDate expiresDate,
                Instant grantedAt
        ) {
            return new GrantedCertification(
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
