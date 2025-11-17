package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.query.FollowQueryRepository;
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

    private final FollowRepository followRepository;
    private final FollowQueryRepository followQueryRepository;

    public Follow get(Long followId, Long playerId) {
        return followRepository.findByIdAndPlayerId(followId, playerId).orElseThrow(() -> new DomainException(
                SocialError.FOLLOW_NOT_FOUND));
    }

    // 목록
    public List<Follow> followings(Long playerId, int page, int size) {
        return followQueryRepository.findFollowings(playerId, page, size);
    }

    public long countFollowings(Long playerId) {
        return followQueryRepository.countFollowings(playerId);
    }

    public List<Follow> followers(Long playerId, int page, int size) {
        return followQueryRepository.findFollowers(playerId, page, size);
    }

    public long countFollowers(Long playerId) {
        return followQueryRepository.countFollowers(playerId);
    }

    public List<Follow> recentFollowings(Long playerId, int limit) {
        return followQueryRepository.recentFollowings(playerId, limit);
    }

    public List<Follow> recentFollowers(Long playerId, int limit) {
        return followQueryRepository.recentFollowers(playerId, limit);
    }

    public boolean isFriend(Long friendId, Long playerId) {
        return followRepository.existsByPlayerIdAndTargetId(playerId, friendId)
                && followRepository.existsByPlayerIdAndTargetId(friendId, playerId);
    }
}
