package online.lifeasgame.character.api.player.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PlayerRequest {

    private PlayerRequest() {
    }

    public record Register(
            @NotBlank @Size(min = 1, max = 40) String name,
            @NotBlank String gender
    ) {
    }
}
