package online.lifeasgame.user.domain.repository;

import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(Long userId);

    boolean existsByEmail(Email email);

    boolean existsByNickname(Nickname nickname);
}
