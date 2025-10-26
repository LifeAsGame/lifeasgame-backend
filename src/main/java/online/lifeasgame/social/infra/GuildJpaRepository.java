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
                        select g.id
                        from Guild g
                        where (:keyword is null or :keyword='' or lower(g.name.value) like lower(concat('%',:keyword,'%'))
                               or lower(g.code.value) like lower(concat('%',:keyword,'%')))
                          and (:visibility is null or g.visibility = :visibility)
                        order by g.id desc
                    """
    )
    Page<Long> searchIds(
            @Param("keyword") String keyword,
            @Param("visibility") GuildVisibility visibility,
            Pageable pageable
    );

    @Query(
            """
                        select distinct g
                        from Guild g
                        left join fetch g.tags t
                        where g.id in :ids
                    """
    )
    List<Guild> fetchWithTagsByIds(@Param("ids") List<Long> ids);

    @Query(
            """
                        select distinct g
                        from Guild g
                        left join fetch g.tags t
                        order by g.id desc
                    """
    )
    List<Guild> findRecentWithTags(Pageable pageable);
}

