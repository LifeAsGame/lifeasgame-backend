package online.lifeasgame.character.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PlayerRequest {

    private PlayerRequest() {
    }

    public record Register(
            @NotBlank @Size(min = 1, max = 20) String name,
            @NotBlank String gender
    ) {
    }
}
