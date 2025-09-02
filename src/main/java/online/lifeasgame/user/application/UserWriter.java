package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.HashedPassword;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.error.UserError;
import online.lifeasgame.user.domain.event.UserRegistered;
import online.lifeasgame.user.domain.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserWriter {

    private final UserRepository userRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long register(Email email, HashedPassword hashedPassword, Nickname nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new DomainException(UserError.EMAIL_DUPLICATE, email.getValue());
        }

        User user = userRepository.save(User.register(email, hashedPassword, nickname));

        domainEventPublisher.publish(
                UserRegistered.of(
                        user.getId(),
                        user.getEmail().getValue(),
                        user.getNickname().getValue()
                )
        );

        return user.getId();
    }
}
