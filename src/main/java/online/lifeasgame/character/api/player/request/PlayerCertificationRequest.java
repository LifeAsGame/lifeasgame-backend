package online.lifeasgame.character.api.player.request;

import java.time.LocalDate;

public final class PlayerCertificationRequest {

    private PlayerCertificationRequest() {
    }

    public record Change(
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
    }

    public record Create(
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
    }
}
