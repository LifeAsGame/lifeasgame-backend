package online.lifeasgame.character.api.admin.request;

import java.time.LocalDate;

public final class AdminPlayerHobbyRequest {

    private AdminPlayerHobbyRequest() {}

    public record Grant(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
    }
}
