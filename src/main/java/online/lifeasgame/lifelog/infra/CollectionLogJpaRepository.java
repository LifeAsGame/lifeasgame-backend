package online.lifeasgame.lifelog.infra;

import online.lifeasgame.lifelog.domain.CollectionCategory;
import online.lifeasgame.lifelog.domain.CollectionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollectionLogJpaRepository extends JpaRepository<CollectionLog, Long> {

    //tag n+1해결
    @Query("""
        select distinct c
        from CollectionLog c
        left join fetch c.tags.values v
        where c.playerId = :playerId
        order by c.id desc
    """)
    List<CollectionLog> findRecentWithTags(@Param("playerId") Long playerId, Pageable pageable);

    Optional<CollectionLog> findByIdAndPlayerId(Long id, Long playerId);

    // n+1 문제 & lower concat 해결 필요할듯
    @Query("""
        select c.id
        from CollectionLog c
        where c.playerId = :playerId
          and (:category is null or c.category = :category)
          and (:titleLike is null or lower(c.title.value) like lower(concat('%', :titleLike, '%')))
        order by c.id desc
    """)
    Page<Long> searchIds(@Param("playerId") Long playerId,
                         @Param("category") CollectionCategory category,
                         @Param("titleLike") String titleLike,
                         Pageable pageable);

    @Query("""
        select distinct c
        from CollectionLog c
        left join fetch c.tags.values v
        where c.id in :ids
        order by c.id desc
    """)
    List<CollectionLog> findAllWithTagsByIdIn(@Param("ids") List<Long> ids);
}
