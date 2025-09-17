package online.lifeasgame.character.presentation.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PlayerCertificationResponse {

    private PlayerCertificationResponse() {
    }

    public record PlayerCertificationInfos(List<PlayerCertificationInfo> playerCertificationInfos) {
        public static PlayerCertificationInfos of(List<PlayerCertificationInfo> playerCertificationInfos) {
            return new PlayerCertificationInfos(playerCertificationInfos);
        }
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
        public static PlayerCertificationInfo of(
                Long certificationId,
                String name,
                String issuer,
                String category,
                LocalDate acquiredDate,
                LocalDate expiresDate,
                Instant grantedAt
        ) {
            return new PlayerCertificationInfo(
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
