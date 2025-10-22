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

    List<CollectionLog> findByPlayerIdOrderByIdDesc(Long playerId, Pageable pageable);

    Optional<CollectionLog> findByIdAndPlayerId(Long id, Long playerId);

    @Query(
            """
                        SELECT c FROM CollectionLog c
                        WHERE c.playerId = :playerId
                          AND (:category IS NULL OR c.category = :category)
                          AND (:titleLike IS NULL OR LOWER(c.title.value) LIKE LOWER(CONCAT('%', :titleLike, '%')))
                    """
    )
    Page<CollectionLog> search(
            @Param("playerId") Long playerId,
            @Param("category") CollectionCategory category,
            @Param("titleLike") String titleLike,
            Pageable pageable
    );
}
