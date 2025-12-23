package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.FollowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class FollowReader {

    private final FollowRepository repository;

    public Follow getByFollowIdAndPlayerId(Long followId, Long playerId) {
        return repository.findByIdAndPlayerId(followId, playerId).orElseThrow(() -> new DomainException(
                SocialError.FOLLOW_NOT_FOUND));
    }

    public List<Follow> getFollowingsByPlayerId(Long playerId, int page, int size) {
        return repository.findFollowings(playerId, page, size);
    }

    public long countFollowings(Long playerId) {
        return repository.countFollowings(playerId);
    }

    public List<Follow> getFollowersByPlayerId(Long playerId, int page, int size) {
        return repository.findFollowers(playerId, page, size);
    }

    public long countFollowers(Long playerId) {
        return repository.countFollowers(playerId);
    }

    public List<Follow> recentFollowings(Long playerId, int limit) {
        return repository.recentFollowings(playerId, limit);
    }

    public List<Follow> recentFollowers(Long playerId, int limit) {
        return repository.recentFollowers(playerId, limit);
    }

    public void assertExistsFriend(Long friendId, Long playerId) {
        if (repository.existsByPlayerIdAndTargetId(playerId, friendId)
                && repository.existsByPlayerIdAndTargetId(friendId, playerId)) {
            throw new DomainException(SocialError.NOT_FRIEND);
        }
    }
}
