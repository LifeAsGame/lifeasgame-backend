package online.lifeasgame.character.api.admin.request;

import java.time.LocalDate;

public final class AdminPlayerCertificationRequest {

    private AdminPlayerCertificationRequest() {}

    public record Grant(
            LocalDate acquiredDate,
            LocalDate expiresDate
    ) {
    }
}
