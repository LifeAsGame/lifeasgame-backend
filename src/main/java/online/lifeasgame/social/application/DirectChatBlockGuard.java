package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.FollowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class DirectChatBlockGuard {

    private final FollowRepository repository;

    public void requireUnblocked(Long playerId, Long peerId) {
        long first = Math.min(playerId, peerId);
        long second = Math.max(playerId, peerId);
        // Canonical lock order for both actors. Scalar current reads avoid stale JPA/snapshot state.
        boolean forwardBlocked = repository.findBlockedForUpdate(first, second).orElse(false);
        boolean reverseBlocked = repository.findBlockedForUpdate(second, first).orElse(false);
        if (forwardBlocked || reverseBlocked) {
            throw new DomainException(SocialError.CHAT_DIRECT_BLOCKED);
        }
    }
}
