package online.lifeasgame.social.infra;

import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuildJpaRepository extends JpaRepository<Guild, Long> {

    Optional<Guild> findByIdAndPlayerId(Long id, Long playerId);

    @Query(
        """
            SELECT g.id
            FROM Guild g
            WHERE (:keyword IS NULL OR :keyword='' OR LOWER(g.name.value) LIKE LOWER(CONCAT('%',:keyword,'%') )
                OR LOWER(g.code.value) LIKE LOWER(CONCAT('%',:keyword,'%') ) )
                AND (:visibility IS NULL OR g.visibility = :visibility)
            ORDER BY g.id DESC
        """
    )
    Page<Long> searchIds(
            @Param("keyword") String keyword,
            @Param("visibility") GuildVisibility visibility,
            Pageable pageable
    );

    @Query(
        """
            SELECT DISTINCT g
            FROM Guild g
            LEFT JOIN FETCH g.tags t
            WHERE g.id IN :ids
        """
    )
    List<Guild> fetchWithTagsByIds(@Param("ids") List<Long> ids);

    @Query(
        """
            SELECT DISTINCT g
            FROM Guild g
            LEFT JOIN FETCH g.tags t
            WHERE g.id IN :ids
        """
    )
    List<Guild> findRecentWithTags(List<Long> ids);

    @Query(
            value =
                """
                    SELECT g.id
                    FROM Guild g
                    ORDER BY g.createdAt
                    LIMIT :limits
                """
            , nativeQuery = true
    )
    List<Long> findRecent(@Param("limits") Integer limits);
}
