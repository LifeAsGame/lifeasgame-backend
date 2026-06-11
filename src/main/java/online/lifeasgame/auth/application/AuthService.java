package online.lifeasgame.auth.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;

    public AuthResult.TokenPair issueToken(Long userId, Long playerId) {
        return new AuthResult.TokenPair(
                jwtProvider.createAccessToken(userId, playerId),
                jwtProvider.createRefreshToken(userId),
                userId, playerId
        );
    }

    public AuthResult.TokenPair reissueToken(String refreshToken, Long playerId) {
        Long userId = jwtProvider.extractUserId(refreshToken)
                .orElseThrow(() -> new AuthException(AuthError.TOKEN_INVALID));
        return new AuthResult.TokenPair(
                jwtProvider.createAccessToken(userId, playerId),
                jwtProvider.createRefreshToken(userId),
                userId, playerId
        );
    }
}
