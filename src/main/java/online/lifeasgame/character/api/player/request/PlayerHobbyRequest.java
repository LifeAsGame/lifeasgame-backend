package online.lifeasgame.character.api.player.request;

import online.lifeasgame.platform.web.validation.DatabaseCalendarDate;
import java.time.LocalDate;

public final class PlayerHobbyRequest {

    private PlayerHobbyRequest() {
    }

    public record Create(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            @DatabaseCalendarDate LocalDate startedOn
    ) {
    }

    public record Update(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            @DatabaseCalendarDate LocalDate startedOn
    ) {
    }
}
