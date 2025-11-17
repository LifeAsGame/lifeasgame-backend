package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.Follow;

import java.util.Optional;

public interface FollowRepository {
    Follow save(Follow f);

    Optional<Follow> findById(Long id);

    Optional<Follow> findByIdAndPlayerId(Long id, Long playerId);

    boolean existsByPlayerIdAndTargetId(Long playerId, Long friendId);
}
