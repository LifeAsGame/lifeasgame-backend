package online.lifeasgame.social.api.admin.request;

import jakarta.validation.constraints.NotNull;

public final class AdminFollowRequest {

    private AdminFollowRequest() {
    }

    public record Create(@NotNull Long targetPlayerId) {
    }
}

