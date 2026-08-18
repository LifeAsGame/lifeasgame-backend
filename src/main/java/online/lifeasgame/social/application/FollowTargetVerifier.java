package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerExistenceReadApi;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class FollowTargetVerifier {

    private final PlayerExistenceReadApi playerExistenceReadApi;

    public void verifyExists(Long targetPlayerId) {
        if (!playerExistenceReadApi.existsByPlayerId(targetPlayerId)) {
            throw new DomainException(SocialError.FOLLOW_TARGET_NOT_FOUND);
        }
    }
}
