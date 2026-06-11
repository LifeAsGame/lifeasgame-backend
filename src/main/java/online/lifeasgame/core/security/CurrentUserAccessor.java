package online.lifeasgame.core.security;

import java.util.Optional;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;

public interface CurrentUserAccessor {
    Optional<Long> currentUserId();

    default Long currentUserIdOrThrow() {
        return currentUserId().orElseThrow(() -> new AuthException(AuthError.UNAUTHORIZED));
    }
}
