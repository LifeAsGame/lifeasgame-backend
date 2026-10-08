package online.lifeasgame.demo.api;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.demo.application.DemoActorScopeApi;
import online.lifeasgame.demo.application.DemoRunStore;
import online.lifeasgame.demo.domain.DemoError;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class DemoActorHttpGate extends OncePerRequestFilter {
    private final DemoRunStore store;
    private final DemoActorScopeApi scope;
    private final JwtProvider jwt;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String bearer = request.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        Claims claims = jwt.parseAccessToken(bearer.substring(7)).orElse(null);
        if (claims == null) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Long userId = Long.valueOf(claims.getSubject());
            DemoRunStore.Actor actor = store.actorByUser(userId);
            if (actor != null) {
                Boolean provisioning = claims.get("demoProvisioning", Boolean.class);
                boolean internal = Boolean.TRUE.equals(provisioning)
                        && ("127.0.0.1".equals(request.getRemoteAddr()) || "::1".equals(request.getRemoteAddr()));
                if (!internal) {
                    scope.requireActive(userId, claims.get("pid", Long.class));
                    if (!allowed(request.getMethod(), request.getRequestURI())) {
                        throw new DomainException(DemoError.ACTOR_FORBIDDEN);
                    }
                }
            }
        } catch (DomainException exception) {
            DemoError code = (DemoError) exception.getErrorCode();
            response.setStatus(code.status());
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"title\":\"" + code.code() + "\",\"status\":" + code.status() +
                    ",\"code\":\"" + code.code() + "\"}");
            return;
        } catch (IllegalArgumentException exception) {
            response.sendError(401);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean allowed(String method, String path) {
        if (path.equals("/api/v1/users/me") || path.equals("/api/v1/players") ||
                path.startsWith("/api/v1/players/growth") ||
                path.startsWith("/api/v1/players/activated-content") ||
                path.startsWith("/api/v1/players/titles") ||
                path.startsWith("/api/v1/players/achievements") ||
                path.startsWith("/api/v1/players/quests") ||
                path.startsWith("/api/v1/quest-routes") ||
                (path.equals("/api/v1/roles") || path.matches("/api/v1/roles/[0-9]+")) ||
                path.startsWith("/api/v1/lifelogs") ||
                path.startsWith("/api/v1/players/collections") ||
                path.startsWith("/api/v1/inventory") ||
                path.startsWith("/api/v1/mailbox") ||
                (path.equals("/api/v1/chat/channels") ||
                        path.equals("/api/v1/chat/channels/friends") ||
                        path.matches("/api/v1/chat/channels/friend/[0-9]+") ||
                        path.matches("/api/v1/chat/channels/[0-9]+/(messages|read)")) ||
                path.startsWith("/api/v1/follows") ||
                path.startsWith("/api/v1/catalog") ||
                path.startsWith("/api/v1/notifications")) return true;
        if (path.equals("/api/v1/economy/wallet") || path.equals("/api/v1/economy/trades") ||
                path.equals("/api/v1/economy/listings") ||
                path.equals("/api/v1/economy/listings/me") ||
                path.equals("/api/v1/economy/listings/reservations") ||
                path.matches("/api/v1/economy/listings/[0-9]+(/reserve|/purchase)?")) return true;
        return method.equals("POST") && path.equals("/api/v1/auth/refresh");
    }
}
