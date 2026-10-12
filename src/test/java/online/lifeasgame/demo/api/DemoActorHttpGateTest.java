package online.lifeasgame.demo.api;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.demo.application.DemoActorScopeApi;
import online.lifeasgame.demo.application.DemoProperties;
import online.lifeasgame.demo.application.DemoRunStore;
import online.lifeasgame.demo.domain.DemoError;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DemoActorHttpGateTest {
    @Test
    @DisplayName("OFF에서는 루프백 준비용 토큰도 보호 API를 통과하지 못한다")
    void disabledProvisioningToken() throws Exception {
        DemoRunStore store = mock(DemoRunStore.class);
        DemoActorScopeApi scope = mock(DemoActorScopeApi.class);
        JwtProvider jwt = mock(JwtProvider.class);
        Claims claims = mock(Claims.class);
        FilterChain chain = mock(FilterChain.class);
        when(jwt.parseAccessToken("token")).thenReturn(Optional.of(claims));
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("pid", Long.class)).thenReturn(2L);
        when(claims.get("demoProvisioning", Boolean.class)).thenReturn(true);
        when(store.actorByUser(1L)).thenReturn(new DemoRunStore.Actor("run", "explorer", 1L, 2L));
        doThrow(new DomainException(DemoError.ACTOR_FORBIDDEN)).when(scope).requireActive(1L, 2L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/players");
        request.addHeader("Authorization", "Bearer token");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new DemoActorHttpGate(store, scope, new DemoProperties(false, 24, 20, 30, 500), jwt)
                .doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("ON에서는 루프백 준비용 토큰으로 기존 준비 작업을 진행한다")
    void enabledProvisioningToken() throws Exception {
        DemoRunStore store = mock(DemoRunStore.class);
        DemoActorScopeApi scope = mock(DemoActorScopeApi.class);
        JwtProvider jwt = mock(JwtProvider.class);
        Claims claims = mock(Claims.class);
        FilterChain chain = mock(FilterChain.class);
        when(jwt.parseAccessToken("token")).thenReturn(Optional.of(claims));
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("demoProvisioning", Boolean.class)).thenReturn(true);
        when(store.actorByUser(1L)).thenReturn(new DemoRunStore.Actor("run", "explorer", 1L, 2L));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/players");
        request.addHeader("Authorization", "Bearer token");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new DemoActorHttpGate(store, scope, new DemoProperties(true, 24, 20, 30, 500), jwt)
                .doFilter(request, response, chain);

        verifyNoInteractions(scope);
        verify(chain).doFilter(request, response);
    }
}
