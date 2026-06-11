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
    }

    public record Update(
            String customName,
            String detail,
            Integer proficiency,
            String status,
            LocalDate startedOn
    ) {
    }
}
