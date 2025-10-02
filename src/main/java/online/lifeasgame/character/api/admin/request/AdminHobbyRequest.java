package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;

public class AdminHobbyRequest {

    private AdminHobbyRequest() {
    }

    public record Create(
            @NotBlank String name,
            @NotBlank String category
    ) {
        public static Create of(String name, String category) {
            return new Create(name, category);
        }
    }
}
