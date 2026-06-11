package online.lifeasgame.social.api.player.request;

import jakarta.validation.constraints.NotNull;

public final class PlayerFollowRequest {

    private PlayerFollowRequest() {
    }

    public record Create(@NotNull Long targetPlayerId) {
    }
}
