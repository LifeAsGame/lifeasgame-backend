package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.user.application.query.UserQueryRepository;
import online.lifeasgame.user.application.query.UserSearchQuery;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserStatus;
import online.lifeasgame.user.domain.error.UserError;
import online.lifeasgame.user.domain.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class UserReader {

    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;

    public User findByIdOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserError.USER_NOT_FOUND));
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public User findByEmailOrElseThrow(String emailStr) {
        return userRepository.findByEmail(Email.of(emailStr))
                .orElseThrow(() -> new DomainException(UserError.USER_NOT_FOUND));
    }

    public Optional<User> findByEmail(String emailStr) {
        return userRepository.findByEmail(Email.of(emailStr));
    }

    public boolean existsByEmail(Email email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByNickname(Nickname nickname) {
        return userRepository.existsByNickname(nickname);
    }

    public UserSearchQuery.SearchResult search(String email, String nickname, UserStatus status, int page, int size) {
        return userQueryRepository.search(email, nickname, status, page, size);
    }
}
