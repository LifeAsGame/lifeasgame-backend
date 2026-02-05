package online.lifeasgame.character.application.result;

import online.lifeasgame.character.application.view.PlayerCertificationView;
import online.lifeasgame.character.domain.Certification;
import online.lifeasgame.character.domain.PlayerCertification;

import java.time.Instant;
import java.time.LocalDate;

public final class PlayerCertificationResult {

    private PlayerCertificationResult() {
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
        public static Info from(PlayerCertificationView v) {
            return new Info(
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

    public record Changed(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static Changed from(PlayerCertification playerCertification) {
            return new Changed(
                    playerCertification.getCertificationId(),
                    playerCertification.getAcquiredDate(),
                    playerCertification.getExpiresDate()
            );
        }
    }

    public record Created(
            Long playerId,
            Long certificationId,
            String name,
            String issuer,
            String category,
            LocalDate acquiredDate,
            LocalDate expiresDate,
            Instant grantedAt
    ) {
        public static Created from(PlayerCertification playerCertification, Certification certification) {
            return new PlayerCertificationResult.Created(
                    playerCertification.getPlayerId(),
                    playerCertification.getCertificationId(),
                    certification.getName(),
                    certification.getIssuer(),
                    certification.getCategory().name(),
                    playerCertification.getAcquiredDate(),
                    playerCertification.getExpiresDate(),
                    playerCertification.getGrantedAt()
            );
        }
    }

    public record Revoked(Long playerId, Long certificationId) {}
}
