package online.lifeasgame.social.infra;

import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyJpaRepository extends JpaRepository<Party, Long> {

    Optional<Party> findByIdAndPlayerId(Long id, Long playerId);

    @Query(
        """
            SELECT p.id
            FROM Party p
            WHERE (:keyword IS NULL OR :keyword='' OR LOWER(p.name.value) LIKE LOWER(CONCAT('%',:keyword,'%') )
                OR LOWER(p.code.value) LIKE LOWER(CONCAT('%',:keyword,'%') ) )
                AND (:visibility IS NULL OR p.visibility = :visibility)
            ORDER BY p.id DESC
        """
    )
    Page<Long> searchIds(
            @Param("keyword") String keyword,
            @Param("visibility") PartyVisibility visibility,
            Pageable pageable
    );

    @Query(
        """
            SELECT DISTINCT p
            FROM Party p
            LEFT JOIN FETCH p.tags t
            WHERE p.id IN :ids
        """
    )
    List<Party> fetchWithTagsByIds(@Param("ids") List<Long> ids);

    @Query(
        """
            SELECT DISTINCT p
            FROM Party p
            LEFT JOIN FETCH p.tags t
            WHERE p.id IN :ids
        """
    )
    List<Party> findRecentWithTags(@Param("ids") List<Long> ids);

    @Query(
        """
            SELECT p.id
            FROM Party p
            ORDER BY p.createdAt
            LIMIT :limits
        """
    )
    List<Long> findRecent(@Param("limits") Integer limits);
}
