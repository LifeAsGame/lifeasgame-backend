package online.lifeasgame.character.presentation.request;

import java.time.LocalDate;

public class AdminPlayerHobbyRequest {

    private AdminPlayerHobbyRequest() {
    }

    public record GrantHobby(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn,
            Long xp
    ) {
        public static GrantHobby of(
                String customName,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn,
                Long xp
        ) {
            return new GrantHobby(customName, detail, proficiency, status, startedOn, xp);
        }
    }
}
