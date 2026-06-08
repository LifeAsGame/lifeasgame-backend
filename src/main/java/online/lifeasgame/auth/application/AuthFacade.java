package online.lifeasgame.auth.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.character.application.PlayerService;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.user.application.UserService;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.result.UserResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {

    private final UserService userService;
    private final PlayerService playerService;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public AuthResult.TokenPair login(String email, String password) {
        UserResult.AuthCredential credential =
                userService.findAuthCredential(email, password);
        Long playerId = playerService.findPlayerIdByUserId(credential.userId());
        return authService.issueToken(credential.userId(), playerId);
    }

    public AuthResult.RegisterResult register(String email, String password, String nickname) {
        UserResult.Created created = userService.register(
                new UserCommand.Register(email, password, nickname));
        Long playerId = playerService.findPlayerIdByUserId(created.id());
        return AuthResult.RegisterResult.verified(
                authService.issueToken(created.id(), playerId));
        // 메일 인증 활성화 시 교체:
        // return AuthResult.RegisterResult.pendingVerification();
    }

    public AuthResult.TokenPair refresh(String refreshToken) {
        Long userId = jwtProvider.extractUserId(refreshToken)
                .orElseThrow(() -> new AuthException(AuthError.TOKEN_INVALID));
        Long playerId = playerService.findPlayerIdByUserId(userId);
        return authService.reissueToken(refreshToken, playerId);
    }
}
