package online.lifeasgame.platform.security;

import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
//@Profile({"local", "dev", "test"})
@Profile("test")
public class FixedDevCurrentPlayerAccessor implements CurrentPlayerAccessor {
    @Override
    public Optional<Long> currentPlayerId() {
        return Optional.of(6L);
    }
}
