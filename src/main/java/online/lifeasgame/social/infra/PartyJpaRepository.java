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
                        select p.id
                        from Party p
                        where (:keyword is null or :keyword='' or lower(p.name.value) like lower(concat('%',:keyword,'%'))
                               or lower(p.code.value) like lower(concat('%',:keyword,'%')))
                          and (:visibility is null or p.visibility = :visibility)
                        order by p.id desc
                    """
    )
    Page<Long> searchIds(
            @Param("keyword") String keyword,
            @Param("visibility") PartyVisibility visibility,
            Pageable pageable
    );

    @Query(
            """
                        select distinct p
                        from Party p
                        left join fetch p.tags t
                        where p.id in :ids
                    """
    )
    List<Party> fetchWithTagsByIds(@Param("ids") List<Long> ids);

    @Query(
            """
                        select distinct p
                        from Party p
                        left join fetch p.tags t
                        where p.id in :ids
                    """
    )
    List<Party> findRecentWithTags(@Param("ids") List<Long> ids);

    @Query(
            """
                SELECT p.id
                FROM Party p
                ORDER BY p.createdAt
                LIMIT :limits
            """)
    List<Long> findRecent(@Param("limits") Integer limits);
}
