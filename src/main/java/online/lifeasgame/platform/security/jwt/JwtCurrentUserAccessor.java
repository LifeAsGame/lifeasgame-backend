package online.lifeasgame.platform.security.jwt;

import online.lifeasgame.core.security.CurrentUserAccessor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile({"prod", "local"})
public class JwtCurrentUserAccessor implements CurrentUserAccessor {

    @Override
    public Optional<Long> currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        if (auth.getPrincipal() instanceof JwtPrincipal p) return Optional.of(p.userId());
        return Optional.empty();
    }
}
