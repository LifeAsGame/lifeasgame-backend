package online.lifeasgame.character.application.command;

import java.time.LocalDate;

public class AdminPlayerCertificationCommand {

    private AdminPlayerCertificationCommand() {
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
