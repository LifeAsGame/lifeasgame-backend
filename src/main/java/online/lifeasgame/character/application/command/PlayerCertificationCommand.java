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
    }

    public record Create(
            Long certificationId,
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
    }
}
