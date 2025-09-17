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

    public record ChangedPlayerCertification(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static ChangedPlayerCertification of(
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new ChangedPlayerCertification(
                    certificationId,
                    acquiredDate,
                    expiresDate
            );
        }
    }

    public record CreatedPlayerCertification(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static CreatedPlayerCertification of(
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new CreatedPlayerCertification(
                    certificationId,
                    acquiredDate,
                    expiresDate
            );
        }
    }
}
