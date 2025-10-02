package online.lifeasgame.character.api.player.request;

import java.time.LocalDate;

public class PlayerCertificationRequest {

    private PlayerCertificationRequest() {
    }

    public record Change(
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static Change of(
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new Change(acquiredDate, expiresDate);
        }
    }

    public record Create(
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static Create of(
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new Create(
                    acquiredDate,
                    expiresDate
            );
        }
    }
}
