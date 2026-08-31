package online.lifeasgame.character.api.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminPlayerHolderGrantRequest {

    private AdminPlayerHolderGrantRequest() {
    }

    public record Grant(
            @NotBlank
            @Size(max = 512)
            @Pattern(regexp = "(?=.*[^\\p{Cf}\\p{Zs}])[^\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]*")
            String reason
    ) {
    }
}
