package online.lifeasgame.user.infra;

import online.lifeasgame.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail_Value(String emailValue);
}
