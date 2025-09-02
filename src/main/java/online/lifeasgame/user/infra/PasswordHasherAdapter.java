package online.lifeasgame.user.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.application.PasswordHasher;
import online.lifeasgame.user.application.model.RawPassword;
import online.lifeasgame.user.domain.HashedPassword;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordHasherAdapter implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public HashedPassword hash(RawPassword raw) {
        return HashedPassword.of(passwordEncoder.encode(raw.value()));
    }

    @Override
    public boolean matches(RawPassword raw, HashedPassword hashed) {
        return passwordEncoder.matches(raw.value(), hashed.getValue());
    }
}
