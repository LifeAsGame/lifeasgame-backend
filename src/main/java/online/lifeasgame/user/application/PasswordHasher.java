package online.lifeasgame.user.application;

import online.lifeasgame.user.application.model.RawPassword;
import online.lifeasgame.user.domain.HashedPassword;

public interface PasswordHasher {
    HashedPassword hash(RawPassword raw);
    boolean matches(RawPassword raw, HashedPassword hashed);
}
