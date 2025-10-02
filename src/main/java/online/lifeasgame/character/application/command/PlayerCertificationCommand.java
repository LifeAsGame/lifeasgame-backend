package online.lifeasgame.character.application.command;

import java.time.LocalDate;

public class PlayerCertificationCommand {

    private PlayerCertificationCommand() {
    }

    public record ChangePlayerCertification(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static ChangePlayerCertification of(
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new ChangePlayerCertification(certificationId, acquiredDate, expiresDate);
        }
    }

    public record CreatePlayerCertification(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static CreatePlayerCertification of(
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new CreatePlayerCertification(certificationId, acquiredDate, expiresDate);
        }
    }

    public record GrantCertification(
            Long playerId,
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static GrantCertification of(
                Long playerId,
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new GrantCertification(playerId, certificationId, acquiredDate, expiresDate);
        }
    }
}
