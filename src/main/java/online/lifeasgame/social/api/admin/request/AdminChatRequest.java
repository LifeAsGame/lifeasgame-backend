package online.lifeasgame.social.api.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminChatRequest {

    private AdminChatRequest() {
    }

    public record OpenAdmin(
            @Size(max = 60) String name
    ) {
    }

    public record SendMessage(
            @NotBlank String content
    ) {
    }
}
