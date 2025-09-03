package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.error.UserError;
import online.lifeasgame.user.domain.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(UserError.USER_NOT_FOUND));
    }
}
