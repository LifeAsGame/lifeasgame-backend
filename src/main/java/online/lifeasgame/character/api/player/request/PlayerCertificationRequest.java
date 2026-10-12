package online.lifeasgame.character.api.player.request;

import online.lifeasgame.platform.web.validation.DatabaseCalendarDate;
import java.time.LocalDate;

public final class PlayerCertificationRequest {

    private PlayerCertificationRequest() {
    }

    public record Update(
            @DatabaseCalendarDate LocalDate acquiredDate,
            @DatabaseCalendarDate LocalDate expiresDate
    ) {
    }

    public record Create(
            @DatabaseCalendarDate LocalDate acquiredDate,
            @DatabaseCalendarDate LocalDate expiresDate
    ) {
    }
}
