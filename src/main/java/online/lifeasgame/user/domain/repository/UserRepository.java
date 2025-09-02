package online.lifeasgame.user.domain.repository;

import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.User;

public interface UserRepository {
    boolean existsByEmail(Email email);

    User save(User user);
}
