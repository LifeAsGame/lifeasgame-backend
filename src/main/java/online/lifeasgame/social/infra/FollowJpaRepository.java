package online.lifeasgame.social.infra;

import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.FollowState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowJpaRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByIdAndPlayerId(Long id, Long playerId);

    Optional<Follow> findByPlayerIdAndTargetPlayerId(Long playerId, Long targetPlayerId);

    @Query(
        """
            SELECT f.id
            FROM Follow f
            WHERE f.playerId = :playerId
              AND f.state = :state
            ORDER BY f.id DESC
        """
    )
    Page<Long> findFollowingIds(
            @Param("playerId") Long playerId,
            @Param("state") FollowState state,
            Pageable pageable
    );

    @Query(
        """
            SELECT f.id
            FROM Follow f
            WHERE f.targetPlayerId = :playerId
              AND f.state = :state
            ORDER BY f.id DESC
        """
    )
    Page<Long> findFollowerIds(
            @Param("playerId") Long playerId,
            @Param("state") FollowState state,
            Pageable pageable
    );

    @Query(
        """
            SELECT f
            FROM Follow f
            WHERE f.id IN :ids
              AND f.state = :state
        """
    )
    List<Follow> findAllByIdInAndState(
            @Param("ids") List<Long> ids,
            @Param("state") FollowState state
    );

    @Query(
        """
            SELECT f
            FROM Follow f
            WHERE f.playerId = :playerId
              AND f.state = :state
            ORDER BY f.id DESC
        """
    )
    List<Follow> findRecentFollowings(
            @Param("playerId") Long playerId,
            @Param("state") FollowState state,
            Pageable pageable
    );

    @Query(
        """
            SELECT f
            FROM Follow f
            WHERE f.targetPlayerId = :playerId
              AND f.state = :state
            ORDER BY f.id DESC
        """
    )
    List<Follow> findRecentFollowers(
            @Param("playerId") Long playerId,
            @Param("state") FollowState state,
            Pageable pageable
    );

    long countByPlayerIdAndState(Long playerId, FollowState state);

    long countByTargetPlayerIdAndState(Long targetPlayerId, FollowState state);

    boolean existsByPlayerIdAndTargetPlayerIdAndState(
            Long playerId,
            Long targetPlayerId,
            FollowState state
    );
}
