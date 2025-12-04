package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;

public final class AdminHobbyRequest {

    private AdminHobbyRequest() {}

    public record Create(
            @NotBlank String name,
            @NotBlank String category
    ) {
    }
}
