package online.lifeasgame.auth.application;

import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.character.application.internal.PlayerLookupApi;
import online.lifeasgame.core.error.AuthException;
import online.lifeasgame.core.error.api.AuthError;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthFacadeTest {

    @Mock UserAuthApi userAuthApi;
    @Mock PlayerLookupApi playerLookupApi;
    @Mock AuthService authService;
    @Mock JwtProvider jwtProvider;
    @InjectMocks AuthFacade authFacade;

    private static final AuthResult.TokenPair PAIR =
            new AuthResult.TokenPair("access", "refresh", 1L, 2L);

    @Nested @DisplayName("login()")
    class Login {

        @Test @DisplayName("정상 → UserAuthApi → PlayerLookupApi → AuthService 순서")
        void success() {
            InOrder order = inOrder(userAuthApi, playerLookupApi, authService);
            when(userAuthApi.authenticate("e@e.com","pw")).thenReturn(1L);
            when(playerLookupApi.findPlayerIdByUserId(1L)).thenReturn(2L);
            when(authService.issueToken(1L,2L)).thenReturn(PAIR);

            AuthResult.TokenPair r = authFacade.login("e@e.com","pw");

            order.verify(userAuthApi).authenticate("e@e.com","pw");
            order.verify(playerLookupApi).findPlayerIdByUserId(1L);
            order.verify(authService).issueToken(1L,2L);
            assertThat(r.playerId()).isEqualTo(2L);
        }

        @Test @DisplayName("플레이어 없음 → playerId=null")
        void noPlayer_nullPlayerId() {
            when(userAuthApi.authenticate(any(),any())).thenReturn(1L);
            when(playerLookupApi.findPlayerIdByUserId(1L)).thenReturn(null);
            when(authService.issueToken(1L,null))
                    .thenReturn(new AuthResult.TokenPair("a","r",1L,null));

            assertThat(authFacade.login("e@e.com","pw").playerId()).isNull();
        }

        @Test @DisplayName("UserAuthApi 예외 → 전파, PlayerLookupApi 미호출")
        void userAuthApiThrows_propagated() {
            when(userAuthApi.authenticate(any(),any()))
                    .thenThrow(new AuthException(AuthError.BAD_CREDENTIALS));

            assertThatThrownBy(() -> authFacade.login("e@e.com","pw"))
                    .isInstanceOf(AuthException.class);
            verifyNoInteractions(playerLookupApi, authService);
        }
    }

    @Nested @DisplayName("register()")
    class Register {

        @Test @DisplayName("정상 → requiresVerification=false + tokenPair")
        void success() {
            when(userAuthApi.register("e@e.com", "pw", "Nick")).thenReturn(1L);
            when(playerLookupApi.findPlayerIdByUserId(1L)).thenReturn(null);
            when(authService.issueToken(1L,null)).thenReturn(PAIR);

            AuthResult.RegisterResult r = authFacade.register("e@e.com","pw","Nick");

            assertThat(r.requiresVerification()).isFalse();
            assertThat(r.tokenPair()).isNotNull();
        }
    }

    @Nested @DisplayName("refresh()")
    class Refresh {

        @Test @DisplayName("linkStart 후 refresh → playerId 포함 토큰 재발급")
        void afterLinkStart_playerId() {
            when(jwtProvider.extractUserId("rt")).thenReturn(Optional.of(1L));
            when(playerLookupApi.findPlayerIdByUserId(1L)).thenReturn(2L);  // linkStart 완료
            when(authService.reissueToken("rt",2L)).thenReturn(PAIR);

            AuthResult.TokenPair r = authFacade.refresh("rt");

            assertThat(r.playerId()).isEqualTo(2L);
        }

        @Test @DisplayName("linkStart 전 refresh → playerId=null")
        void beforeLinkStart_nullPlayerId() {
            when(jwtProvider.extractUserId("rt")).thenReturn(Optional.of(1L));
            when(playerLookupApi.findPlayerIdByUserId(1L)).thenReturn(null);
            when(authService.reissueToken("rt",null))
                    .thenReturn(new AuthResult.TokenPair("a","r",1L,null));

            assertThat(authFacade.refresh("rt").playerId()).isNull();
        }

        @Test @DisplayName("유효하지 않은 refreshToken → 예외")
        void invalid_throws() {
            when(jwtProvider.extractUserId("bad")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authFacade.refresh("bad"))
                    .isInstanceOf(AuthException.class);
            verifyNoInteractions(authService);
        }
    }
}
