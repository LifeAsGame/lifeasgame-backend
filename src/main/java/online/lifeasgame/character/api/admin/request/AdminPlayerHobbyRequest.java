package online.lifeasgame.character.api.admin.request;

import java.time.LocalDate;

public class AdminPlayerHobbyRequest {

    private AdminPlayerHobbyRequest() {
    }

    public record GrantHobby(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
        public static GrantHobby of(
                String customName,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new GrantHobby(customName, detail, proficiency, status, startedOn);
        }
    }
}
