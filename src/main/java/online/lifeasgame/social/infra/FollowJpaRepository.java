package online.lifeasgame.social.infra;

import online.lifeasgame.social.domain.Follow;
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

    @Query(
        """
            SELECT f.id
            FROM Follow f
            WHERE f.playerId = :playerId
            ORDER BY f.id DESC
        """
    )
    Page<Long> findFollowingIds(@Param("playerId") Long playerId, Pageable pageable);

    @Query(
        """
            SELECT f.id
            FROM Follow f
            WHERE f.targetPlayerId = :playerId
            ORDER BY f.id DESC
        """
    )
    Page<Long> findFollowerIds(@Param("playerId") Long playerId, Pageable pageable);

    @Query(
        """
            SELECT f
            FROM Follow f
            WHERE f.id IN :ids
        """
    )
    List<Follow> findAllByIdIn(@Param("ids") List<Long> ids);

    @Query(
        """
            SELECT f
            FROM Follow f
            WHERE f.playerId = :playerId
            ORDER BY f.id DESC
        """
    )
    List<Follow> findRecentFollowings(@Param("playerId") Long playerId, Pageable pageable);

    @Query(
        """
            SELECT f
            FROM Follow f
            WHERE f.targetPlayerId = :playerId
            ORDER BY f.id DESC
        """
    )
    List<Follow> findRecentFollowers(@Param("playerId") Long playerId, Pageable pageable);

    long countByPlayerId(Long playerId);

    boolean existsByPlayerIdAndTargetPlayerId(Long playerId, Long targetPlayerId);
}
