package online.lifeasgame.core.security;

import java.util.Optional;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;

public interface CurrentPlayerAccessor {
    Optional<Long> currentPlayerId();

    default Long currentPlayerIdOrThrow() {
        return currentPlayerId().orElseThrow(() -> new AuthException(AuthError.UNAUTHORIZED));
    }
}
