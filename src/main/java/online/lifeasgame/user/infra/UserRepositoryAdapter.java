package online.lifeasgame.user.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.user.application.query.UserSearchQuery;
import online.lifeasgame.user.application.query.UserQueryRepository;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserStatus;
import online.lifeasgame.user.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository, UserQueryRepository {

    private final JpaUserRepository jpa;
    private final QuerydslUserRepository querydsl;

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

    @Override
    public boolean existsByNickname(Nickname nickname) {
        return jpa.existsByNickname(nickname);
    }

    @Override
    public UserSearchQuery.SearchResult search(String email, String nickname, UserStatus status, int page, int size) {
        return querydsl.search(email, nickname, status, page, size);
    }
}
