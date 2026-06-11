package online.lifeasgame.lifelog.infra;

import online.lifeasgame.lifelog.domain.MediaCategory;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.WatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MediaLogJpaRepository extends JpaRepository<MediaLog, Long> {

    List<MediaLog> findTop100ByPlayerIdOrderByIdDesc(Long playerId);

    @Query(
            """
                    SELECT m FROM MediaLog m
                    WHERE m.playerId = :playerId
                      AND (:category IS NULL OR m.category = :category)
                      AND (:status IS NULL OR m.status = :status)
                      AND (:titleLike IS NULL OR LOWER(m.title.value) LIKE LOWER(CONCAT('%', :titleLike, '%')))
            """
    )
    Page<MediaLog> search(
            @Param("playerId") Long playerId,
            @Param("category") MediaCategory category,
            @Param("status") WatchStatus status,
            @Param("titleLike") String titleLike,
            Pageable pageable
    );

    Optional<MediaLog> findByIdAndPlayerId(Long id, Long playerId);

    void deleteByIdAndPlayerId(Long id, Long playerId);
}
