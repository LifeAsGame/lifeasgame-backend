package online.lifeasgame.platform.security;

import online.lifeasgame.core.security.CurrentUserAccessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
//@Profile({"local", "dev", "test"})
@Profile("test")
public class FixedDevCurrentUserAccessor implements CurrentUserAccessor {

    @Override
    public Optional<Long> currentUserId() {
        return Optional.of(6L);
    }
}
