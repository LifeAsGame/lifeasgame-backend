package online.lifeasgame.character.api.admin.request;

import java.time.LocalDate;

public class AdminPlayerHobbyRequest {

    private AdminPlayerHobbyRequest() {
    }

    public record Grant(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
        public static Grant of(
                String customName,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new Grant(customName, detail, proficiency, status, startedOn);
        }
    }
}
