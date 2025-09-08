package online.lifeasgame.character.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AdminPlayerRequest {

    private AdminPlayerRequest() {
    }

    public record GrantExp(
            @NotNull @Positive Long playerId,
            @NotNull @Positive Long exp
    ) {
    }
}
