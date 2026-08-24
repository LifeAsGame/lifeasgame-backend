package online.lifeasgame.auth.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.auth.application.internal.AuthTokenApi;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthTokenApi {

    private final JwtProvider jwtProvider;

    @Override
    public AuthResult.TokenPair issueToken(Long userId, Long playerId) {
        return new AuthResult.TokenPair(
                jwtProvider.createAccessToken(userId, playerId),
                jwtProvider.createRefreshToken(userId),
                userId, playerId
        );
    }

    public AuthResult.TokenPair reissueToken(Long userId, Long playerId) {
        return new AuthResult.TokenPair(
                jwtProvider.createAccessToken(userId, playerId),
                jwtProvider.createRefreshToken(userId),
                userId, playerId
        );
    }
}
