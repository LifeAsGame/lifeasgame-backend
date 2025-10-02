package online.lifeasgame.character.application.command;

import java.time.LocalDate;

public final class PlayerCertificationCommand {

    private PlayerCertificationCommand() {
    }

    public record Change(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static Change of(
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new Change(certificationId, acquiredDate, expiresDate);
        }
    }

    public record Create(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static Create of(
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new Create(certificationId, acquiredDate, expiresDate);
        }
    }

    public record Grant(
            Long playerId,
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static Grant of(
                Long playerId,
                Long certificationId,
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new Grant(playerId, certificationId, acquiredDate, expiresDate);
        }
    }
}
