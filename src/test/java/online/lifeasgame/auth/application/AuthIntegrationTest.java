package online.lifeasgame.auth.application;

import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.support.IntegrationTestConfig;
import online.lifeasgame.user.application.UserService;
import online.lifeasgame.user.application.command.UserCommand;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
@Transactional
class AuthIntegrationTest {

    @Autowired AuthFacade authFacade;
    @Autowired UserService userService;
    @Autowired JwtProvider jwtProvider;

    private static final String EMAIL = "auth@test.com";
    private static final String PW    = "password1";
    private static final String NICK  = "AuthUser";

    @BeforeEach void setUp() {
        userService.register(new UserCommand.Register(EMAIL, PW, NICK));
    }

    @Test @DisplayName("login → 유효한 accessToken, playerId=null (캐릭터 미생성)")
    void login_validToken_nullPlayer() {
        AuthResult.TokenPair pair = authFacade.login(EMAIL, PW);
        assertThat(pair.accessToken()).isNotBlank();
        assertThat(pair.playerId()).isNull();
        assertThat(jwtProvider.extractUserId(pair.accessToken())).isPresent();
    }

    @Test @DisplayName("refresh → 새 accessToken, userId 동일")
    void refresh_sameUserId() {
        AuthResult.TokenPair orig = authFacade.login(EMAIL, PW);
        AuthResult.TokenPair ref  = authFacade.refresh(orig.refreshToken());
        assertThat(jwtProvider.extractUserId(ref.accessToken()))
                .isEqualTo(jwtProvider.extractUserId(orig.accessToken()));
    }

    @Test @DisplayName("잘못된 비밀번호 → AuthException")
    void wrongPassword_throws() {
        assertThatThrownBy(() -> authFacade.login(EMAIL, "wrongpass1"))
                .isInstanceOf(AuthException.class);
    }

    @Test @DisplayName("위변조 refreshToken → AuthException")
    void tamperedRefresh_throws() {
        AuthResult.TokenPair pair = authFacade.login(EMAIL, PW);
        assertThatThrownBy(() -> authFacade.refresh(pair.refreshToken() + "tampered"))
                .isInstanceOf(AuthException.class);
    }
}
