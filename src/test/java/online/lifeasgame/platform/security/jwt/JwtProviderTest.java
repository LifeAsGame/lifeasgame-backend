package online.lifeasgame.platform.security.jwt;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars!!";
    private JwtProvider provider;

    @BeforeEach void setUp() {
        JwtProperties p = new JwtProperties();
        p.setSecret(SECRET); p.setAccessTokenExpiryMs(3_600_000L); p.setRefreshTokenExpiryMs(604_800_000L);
        provider = new JwtProvider(p);
    }

    @Test @DisplayName("accessToken — userId, playerId 추출")
    void accessToken_extractBothIds() {
        String token = provider.createAccessToken(10L, 42L);
        assertThat(provider.extractUserId(token)).contains(10L);
        assertThat(provider.extractPlayerId(token)).contains(42L);
    }

    @Test @DisplayName("accessToken — playerId=null 허용")
    void accessToken_nullPlayerId() {
        String token = provider.createAccessToken(10L, null);
        assertThat(provider.extractPlayerId(token)).isEmpty();
    }

    @Test @DisplayName("refreshToken — userId만 포함")
    void refreshToken_onlyUserId() {
        String token = provider.createRefreshToken(10L);
        assertThat(provider.extractUserId(token)).contains(10L);
        assertThat(provider.extractPlayerId(token)).isEmpty();
        assertThat(provider.parseAccessToken(token)).isEmpty();
    }

    @Test @DisplayName("위변조 → empty")
    void tampered_returnsEmpty() {
        assertThat(provider.parse(provider.createAccessToken(1L, 1L) + "x")).isEmpty();
    }

    @Test @DisplayName("만료 → empty")
    void expired_returnsEmpty() throws InterruptedException {
        JwtProperties p = new JwtProperties(); p.setSecret(SECRET); p.setAccessTokenExpiryMs(1L);
        JwtProvider short$ = new JwtProvider(p);
        String token = short$.createAccessToken(1L, 1L);
        Thread.sleep(10);
        assertThat(short$.parse(token)).isEmpty();
    }
}
