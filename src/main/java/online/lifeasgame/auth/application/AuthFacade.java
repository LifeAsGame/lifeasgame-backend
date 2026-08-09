package online.lifeasgame.auth.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {

    private final UserAuthApi userAuthApi;
    private final PlayerLookupApi playerLookupApi;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public AuthResult.TokenPair login(String email, String password) {
        Long userId = userAuthApi.authenticate(email, password);
        Long playerId = playerLookupApi.findPlayerIdByUserId(userId);
        return authService.issueToken(userId, playerId);
    }

    public AuthResult.RegisterResult register(String email, String password, String nickname) {
        Long userId = userAuthApi.register(email, password, nickname);
        Long playerId = playerLookupApi.findPlayerIdByUserId(userId);
        return AuthResult.RegisterResult.verified(
                authService.issueToken(userId, playerId));
        // 메일 인증 활성화 시 교체:
        // return AuthResult.RegisterResult.pendingVerification();
    }

    public AuthResult.TokenPair refresh(String refreshToken) {
        Long userId = jwtProvider.extractUserId(refreshToken)
                .orElseThrow(() -> new AuthException(AuthError.TOKEN_INVALID));
        Long playerId = playerLookupApi.findPlayerIdByUserId(userId);
        return authService.reissueToken(refreshToken, playerId);
    }
}
