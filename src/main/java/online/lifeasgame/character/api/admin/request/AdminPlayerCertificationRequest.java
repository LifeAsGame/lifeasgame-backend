package online.lifeasgame.character.api.admin.request;

import online.lifeasgame.platform.web.validation.DatabaseCalendarDate;
import java.time.LocalDate;

public final class AdminPlayerCertificationRequest {

    private AdminPlayerCertificationRequest() {}

    public record Create(
            @DatabaseCalendarDate LocalDate acquiredDate,
            @DatabaseCalendarDate LocalDate expiresDate
    ) {
    }
}
