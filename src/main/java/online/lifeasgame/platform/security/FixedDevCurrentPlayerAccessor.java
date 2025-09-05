package online.lifeasgame.platform.security;

import java.util.Optional;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Primary
@Component
@Profile({"local", "dev", "test"})
public class FixedDevCurrentPlayerAccessor implements CurrentPlayerAccessor {
    @Override
    public Optional<Long> currentPlayerId() {
        return Optional.of(6L);
    }
}
