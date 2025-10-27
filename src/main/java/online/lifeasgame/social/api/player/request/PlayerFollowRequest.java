package online.lifeasgame.social.api.player.request;

import jakarta.validation.constraints.NotNull;

public final class PlayerFollowRequest {
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
