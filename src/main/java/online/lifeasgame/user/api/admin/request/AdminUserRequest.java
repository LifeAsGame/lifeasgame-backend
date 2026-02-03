package online.lifeasgame.user.api.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminUserRequest {

    private AdminUserRequest() {}

    public record ChangeStatus(
            @NotBlank String toStatus,
            @NotBlank @Size(max = 200) String reason
    ) {}

    public record ForceChangeNickname(
            @NotBlank @Size(min = 2, max = 20) String nickname,
            @NotBlank @Size(max = 200) String reason
    ) {}
}
