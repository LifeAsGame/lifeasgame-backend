package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.HashedPassword;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.event.UserRegistered;
import online.lifeasgame.user.domain.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserWriter {

    private final UserRepository userRepository;
    private final DomainEventPublisher domainEventPublisher;

    public Long register(Email email, HashedPassword hashedPassword, Nickname nickname) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("");
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
