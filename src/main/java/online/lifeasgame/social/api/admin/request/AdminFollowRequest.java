package online.lifeasgame.social.api.admin.request;

import jakarta.validation.constraints.NotNull;

public final class AdminFollowRequest {
    public record Create(@NotNull Long targetPlayerId) {
        public static Create of(Long targetPlayerId) {
            return new Create(targetPlayerId);
        }
    }

    public record Empty() {
        public static Empty of() {
            return new Empty();
        }
    }
}

