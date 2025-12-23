package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.Follow;

import java.util.List;
import java.util.Optional;

public interface FollowRepository {
    Follow save(Follow f);

    Optional<Follow> findById(Long id);

    Optional<Follow> findByIdAndPlayerId(Long id, Long playerId);

    boolean existsByPlayerIdAndTargetId(Long playerId, Long friendId);

    List<Follow> findFollowings(Long playerId, int page, int size);

    long countFollowings(Long playerId);

    List<Follow> findFollowers(Long playerId, int page, int size);

    long countFollowers(Long playerId);

    List<Follow> recentFollowings(Long playerId, int limit);

    List<Follow> recentFollowers(Long playerId, int limit);
}
