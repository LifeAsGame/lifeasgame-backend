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
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class FollowReader {

    private final FollowRepository repository;

    public Follow getByFollowIdAndPlayerId(Long followId, Long playerId) {
        return repository.findByIdAndPlayerId(followId, playerId).orElseThrow(() -> new DomainException(
                SocialError.FOLLOW_NOT_FOUND));
    }

    public Optional<Follow> findByPlayerIdAndTargetPlayerId(Long playerId, Long targetPlayerId) {
        return repository.findByPlayerIdAndTargetPlayerId(playerId, targetPlayerId);
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

}
