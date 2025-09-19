package online.lifeasgame.character.presentation.request;

import jakarta.validation.constraints.NotBlank;

public class AdminHobbyRequest {

    private AdminHobbyRequest() {
    }

    public record CreateHobby(
            @NotBlank String name,
            @NotBlank String category
    ) {
        public static CreateHobby of(String name, String category) {
            return new CreateHobby(name, category);
        }
    }
}
