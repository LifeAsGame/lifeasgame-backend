package online.lifeasgame.platform.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserAuthApi userAuthApi;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            FilterChain chain
    ) throws ServletException, IOException {

        extractToken(req)
                .flatMap(jwtProvider::parseAccessToken)
                .flatMap(this::parsePrincipal)
                .flatMap(this::resolveAuthentication)
                .ifPresent(this::setAuth);
        chain.doFilter(req, res);
    }

    private Optional<String> extractToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return (StringUtils.hasText(h) && h.startsWith("Bearer "))
                ? Optional.of(h.substring(7)) : Optional.empty();
    }

    private Optional<JwtPrincipal> parsePrincipal(
            Claims claims
    ) {
        try {
            return Optional.of(new JwtPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get("pid", Long.class)
            ));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<ResolvedAuthentication> resolveAuthentication(
            JwtPrincipal principal
    ) {
        return userAuthApi.resolveAuthorization(principal.userId())
                .filter(UserAuthApi.AccountAuthorization::active)
                .map(authorization -> new ResolvedAuthentication(
                        principal,
                        authorization.admin()
                ));
    }

    private void setAuth(ResolvedAuthentication resolved) {
        JwtPrincipal principal = resolved.principal();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null,
                        resolved.admin()
                                ? List.of(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                                : List.of(
                                new SimpleGrantedAuthority("ROLE_USER")
                        )
                )
        );

        MDC.put("userId", String.valueOf(principal.userId()));
        if (principal.playerId() != null) {
            MDC.put("playerId", String.valueOf(principal.playerId()));
        }
    }

    private record ResolvedAuthentication(
            JwtPrincipal principal,
            boolean admin
    ) {
    }
}
