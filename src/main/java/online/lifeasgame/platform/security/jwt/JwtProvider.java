package online.lifeasgame.platform.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties properties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, Long playerId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("pid", playerId)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + properties.getAccessTokenExpiryMs()))
                .signWith(key())
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + properties.getRefreshTokenExpiryMs()))
                .signWith(key())
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<Claims> parseAccessToken(String token) {
        return parse(token).filter(claims ->
                "access".equals(claims.get("type", String.class))
        );
    }

    public Optional<Claims> parseRefreshToken(String token) {
        return parse(token).filter(claims ->
                "refresh".equals(claims.get("type", String.class))
        );
    }

    public Optional<Long> extractRefreshUserId(String token) {
        return parseRefreshToken(token)
                .map(claims -> Long.valueOf(claims.getSubject()));
    }

    public Optional<Long> extractUserId(String token) {
        return parse(token).map(c -> Long.valueOf(c.getSubject()));
    }

    public Optional<Long> extractPlayerId(String token) {
        return parse(token).map(c -> c.get("pid", Long.class));
    }
}
