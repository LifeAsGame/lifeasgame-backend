package online.lifeasgame.platform.security.jwt;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile({"prod", "local"})
public class JwtCurrentPlayerAccessor implements CurrentPlayerAccessor {

    @Override
    public Optional<Long> currentPlayerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        if (auth.getPrincipal() instanceof JwtPrincipal p) {
            return Optional.of(p.playerId());
        }

        return Optional.empty();
    }
}
