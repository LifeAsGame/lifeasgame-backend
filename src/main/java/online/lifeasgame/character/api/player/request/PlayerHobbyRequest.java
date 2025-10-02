package online.lifeasgame.character.api.player.request;

import java.time.LocalDate;

public final class PlayerHobbyRequest {

    private PlayerHobbyRequest() {
    }

    public record Create(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
        public static Create of(
                String customName,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new Create(customName, detail, proficiency, status, startedOn);
        }
    }

    public record Change(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
        public static Change of(
                String customName,
                String detail,
                Integer proficiency,
                String status,
                LocalDate startedOn
        ) {
            return new Change(customName, detail, proficiency, status, startedOn);
        }
    }
}
