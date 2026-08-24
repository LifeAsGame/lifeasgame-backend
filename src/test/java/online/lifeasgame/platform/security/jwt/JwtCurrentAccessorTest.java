package online.lifeasgame.platform.security.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT CurrentUser / CurrentPlayer identity")
class JwtCurrentAccessorTest {

    private final JwtCurrentUserAccessor currentUser =
            new JwtCurrentUserAccessor();
    private final JwtCurrentPlayerAccessor currentPlayer =
            new JwtCurrentPlayerAccessor();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ADMIN authority가 추가되어도 기존 user/player identity를 유지한다")
    void preservesUserAndPlayerIdentity() {
        authenticate(new JwtPrincipal(30001L, 300001L));

        assertThat(currentUser.currentUserId()).contains(30001L);
        assertThat(currentPlayer.currentPlayerId()).contains(300001L);
    }

    @Test
    @DisplayName("Player가 없는 ADMIN도 CurrentUser는 유지하고 CurrentPlayer는 비어 있다")
    void allowsAdminWithoutPlayer() {
        authenticate(new JwtPrincipal(30002L, null));

        assertThat(currentUser.currentUserId()).contains(30002L);
        assertThat(currentPlayer.currentPlayerId()).isEmpty();
    }

    private void authenticate(JwtPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                )
        );
    }
}
