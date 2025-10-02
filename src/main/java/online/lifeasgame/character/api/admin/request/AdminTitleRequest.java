package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;

public class AdminTitleRequest {

    private AdminTitleRequest() {
    }

    public record CreateTitle(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String category,
            @NotBlank String descMd
    ) {
        public static CreateTitle of(String code, String name, String category, String descMd) {
            return new CreateTitle(code, name, category, descMd);
        }
    }
}
