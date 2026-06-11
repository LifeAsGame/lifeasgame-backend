package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.repository.FollowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class FollowWriter {

    private final FollowRepository repository;

    public Follow create(Follow follow) {
        return repository.save(follow);
    }
}
