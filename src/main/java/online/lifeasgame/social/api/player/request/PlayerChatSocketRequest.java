package online.lifeasgame.social.api.player.request;

import jakarta.validation.constraints.NotBlank;

public final class PlayerChatSocketRequest {

    private PlayerChatSocketRequest() {
    }

    public record SendMessage(@NotBlank String content,
                              @jakarta.validation.constraints.Size(max = 80)
                              @jakarta.validation.constraints.Pattern(regexp = "\\S+") String clientMessageId) {
    }
}
