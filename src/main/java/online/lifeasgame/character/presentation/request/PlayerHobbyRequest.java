package online.lifeasgame.character.presentation.request;

import java.time.LocalDate;

public class PlayerHobbyRequest {

    private PlayerHobbyRequest() {
    }

    public record CreatePlayerHobby(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
        public static CreatePlayerHobby of(
                String customName,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new CreatePlayerHobby(customName, detail, proficiency, status, startedOn);
        }
    }

    public record ChangePlayerHobby(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
        public static ChangePlayerHobby of(
                String customName,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new ChangePlayerHobby(customName, detail, proficiency, status, startedOn);
        }
    }
}
