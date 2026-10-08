package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.FollowRepository;
import online.lifeasgame.demo.application.DemoActorScopeApi;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class FriendshipVerifier {

    private final FollowRepository repository;
    private final DemoActorScopeApi demoActorScope;

    public void verify(Long playerId, Long friendId) {
        demoActorScope.requireSameBoundary(playerId, friendId);
        if (!repository.existsActiveFollow(playerId, friendId)
                || !repository.existsActiveFollow(friendId, playerId)) {
            throw new DomainException(SocialError.NOT_FRIEND);
        }
    }
}
