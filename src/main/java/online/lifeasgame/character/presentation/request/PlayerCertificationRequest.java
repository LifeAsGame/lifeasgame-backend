package online.lifeasgame.character.presentation.request;

import java.time.LocalDate;

public class PlayerCertificationRequest {

    private PlayerCertificationRequest() {
    }

    public record ChangePlayerCertification(
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
        public static ChangePlayerCertification of(
                LocalDate acquiredDate,
                LocalDate expiresDate
        ) {
            return new ChangePlayerCertification(acquiredDate, expiresDate);
        }
    }
}
