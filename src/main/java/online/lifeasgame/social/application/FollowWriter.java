package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.model.FollowSpec;
import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.repository.FollowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class FollowWriter {

    private final FollowRepository followRepository;

    public Follow create(FollowSpec.Create spec) {
        return followRepository.save(Follow.create(spec.playerId(), spec.targetPlayerId()));
    }

    public void unfollow(Follow f) {
        f.unfollow();
    }

    public void mute(Follow f) {
        f.mute();
    }

    public void unmute(Follow f) {
        f.unmute();
    }

    public void block(Follow f) {
        f.block();
    }

    public void unblock(Follow f) {
        f.unblock();
    }
}
