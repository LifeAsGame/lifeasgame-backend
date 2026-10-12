package online.lifeasgame.character.api.admin.request;

import online.lifeasgame.platform.web.validation.DatabaseCalendarDate;
import java.time.LocalDate;

public final class AdminPlayerHobbyRequest {

    private AdminPlayerHobbyRequest() {}

    public record Grant(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            @DatabaseCalendarDate LocalDate startedOn
    ) {
    }
}
