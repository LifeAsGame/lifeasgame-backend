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

    // 내가 팔로우하는 사람들의 follow id (2단계 로딩)
    @Query(
            """
                        select f.id
                        from Follow f
                        where f.playerId = :playerId
                        order by f.id desc
                    """
    )
    Page<Long> findFollowingIds(@Param("playerId") Long playerId, Pageable pageable);

    // 나를 팔로우하는 사람들의 follow id (2단계 로딩)
    @Query(
            """
                        select f.id
                        from Follow f
                        where f.targetPlayerId = :playerId
                        order by f.id desc
                    """
    )
    Page<Long> findFollowerIds(@Param("playerId") Long playerId, Pageable pageable);

    // id 집합으로 엔티티 조회
    @Query(
            """
                        select f from Follow f
                        where f.id in :ids
                    """
    )
    List<Follow> findAllByIdIn(@Param("ids") List<Long> ids);

    // 최근
    @Query(
            """
                        select f from Follow f
                        where f.playerId = :playerId
                        order by f.id desc
                    """
    )
    List<Follow> findRecentFollowings(@Param("playerId") Long playerId, Pageable pageable);

    @Query(
            """
                        select f from Follow f
                        where f.targetPlayerId = :playerId
                        order by f.id desc
                    """
    )
    List<Follow> findRecentFollowers(@Param("playerId") Long playerId, Pageable pageable);
}
