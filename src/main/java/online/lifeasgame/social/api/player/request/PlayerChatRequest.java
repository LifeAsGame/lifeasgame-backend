package online.lifeasgame.social.api.player.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PlayerChatRequest {

    private PlayerChatRequest() {
    }

    public record OpenGlobal(
            @Size(max = 60)
            String name
    ) {
    }

    public record OpenFriend(
            @Size(max = 60)
            String name
    ) {
    }

    public record OpenAdmin(
            @Size(max = 60)
            String name
    ) {
    }

    public record SendMessage(
            @NotBlank
            String content
    ) {
    }
}
