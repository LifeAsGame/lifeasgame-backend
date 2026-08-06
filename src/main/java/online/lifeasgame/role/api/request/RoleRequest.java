package online.lifeasgame.role.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class RoleRequest {

    private RoleRequest() {
    }

    public record Create(
            @NotBlank @Size(max = 40) String roleType,
            @NotBlank @Size(max = 60) String name,
            @Size(max = 500) String description
    ) {
    }

    public record Update(
            @NotBlank @Size(max = 40) String roleType,
            @NotBlank @Size(max = 60) String name,
            @Size(max = 500) String description
    ) {
    }
}
