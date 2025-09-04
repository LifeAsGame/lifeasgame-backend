package online.lifeasgame.platform.security;

import java.util.Optional;
import online.lifeasgame.core.security.CurrentUserAccessor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Primary
@Component
@Profile({"local", "dev", "test"})
public class FixedDevCurrentUserAccessor implements CurrentUserAccessor {

    @Override
    public Optional<Long> currentUserId() {
        return Optional.of(6L);
    }
}
