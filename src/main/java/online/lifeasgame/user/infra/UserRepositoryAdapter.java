package online.lifeasgame.user.infra;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        return jpaUserRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return jpaUserRepository.findById(userId);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaUserRepository.existsByEmail_Value(email.getValue());
    }
}
