package online.lifeasgame.auth.application;

import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock JwtProvider jwtProvider;
    @InjectMocks AuthService authService;

    @Test @DisplayName("issueToken → TokenPair 반환")
    void issueToken_success() {
        when(jwtProvider.createAccessToken(1L, 2L)).thenReturn("access");
        when(jwtProvider.createRefreshToken(1L)).thenReturn("refresh");
        AuthResult.TokenPair r = authService.issueToken(1L, 2L);
        assertThat(r.accessToken()).isEqualTo("access");
        assertThat(r.playerId()).isEqualTo(2L);
    }

    @Test @DisplayName("issueToken — playerId=null 허용")
    void issueToken_nullPlayerId() {
        when(jwtProvider.createAccessToken(1L, null)).thenReturn("access");
        when(jwtProvider.createRefreshToken(1L)).thenReturn("refresh");
        assertThat(authService.issueToken(1L, null).playerId()).isNull();
    }

    @Test @DisplayName("reissueToken — 유효한 refresh → 새 TokenPair")
    void reissueToken_success() {
        when(jwtProvider.extractUserId("rt")).thenReturn(Optional.of(1L));
        when(jwtProvider.createAccessToken(1L, 2L)).thenReturn("new-access");
        when(jwtProvider.createRefreshToken(1L)).thenReturn("new-refresh");
        AuthResult.TokenPair r = authService.reissueToken("rt", 2L);
        assertThat(r.accessToken()).isEqualTo("new-access");
    }

    @Test @DisplayName("reissueToken — 유효하지 않은 토큰 → 예외")
    void reissueToken_invalid_throws() {
        when(jwtProvider.extractUserId("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.reissueToken("bad", null))
                .isInstanceOf(AuthException.class);
    }
}
