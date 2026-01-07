package online.lifeasgame.user.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpa;

    @Override
    public User save(User user) {
        return jpa.save(user);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return jpa.findById(userId);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email);
    }
}
