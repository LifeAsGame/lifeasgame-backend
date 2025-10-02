package online.lifeasgame.character.api.player.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PlayerCertificationResponse {

    private PlayerCertificationResponse() {
    }

    public record Infos(List<Info> infos) {
        public static Infos of(List<Info> infos) {
            return new Infos(infos);
        }
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
        public static Info of(
                Long certificationId,
                String name,
                String issuer,
                String category,
                LocalDate acquiredDate,
                LocalDate expiresDate,
                Instant grantedAt
        ) {
            return new Info(
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

    public record Changed(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static Changed of(
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new Changed(
                    certificationId,
                    acquiredDate,
                    expiresDate
            );
        }
    }

    public record Created(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static Created of(
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new Created(
                    certificationId,
                    acquiredDate,
                    expiresDate
            );
        }
    }
}
