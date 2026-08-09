package online.lifeasgame.auth.application.internal;

import online.lifeasgame.auth.application.result.AuthResult;

public interface AuthTokenApi {

    AuthResult.TokenPair issueToken(Long userId, Long playerId);
}
