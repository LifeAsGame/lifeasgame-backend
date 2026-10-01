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

    @Query("SELECT g FROM Guild g JOIN g.members m WHERE m.playerId = :playerId AND g.status = online.lifeasgame.social.domain.GuildStatus.ACTIVE ORDER BY g.id DESC")
    Page<Guild> findMine(@Param("playerId") Long playerId, Pageable pageable);

    @Query("SELECT m FROM GuildMember m WHERE m.guild.id = :guildId ORDER BY m.joinedAt, m.id")
    Page<online.lifeasgame.social.domain.GuildMember> findMembers(@Param("guildId") Long guildId, Pageable pageable);

    @Query("SELECT w FROM GuildWaitMember w WHERE w.guild.id = :guildId AND w.type = online.lifeasgame.social.domain.GuildWaitType.JOIN_REQUEST AND w.status = online.lifeasgame.social.domain.GuildWaitStatus.PENDING ORDER BY w.id")
    Page<online.lifeasgame.social.domain.GuildWaitMember> findPendingRequests(@Param("guildId") Long guildId, Pageable pageable);

    @Query("SELECT w FROM GuildWaitMember w JOIN FETCH w.guild g WHERE w.playerId = :playerId AND w.type = :type AND w.status = online.lifeasgame.social.domain.GuildWaitStatus.PENDING ORDER BY w.id DESC")
    Page<online.lifeasgame.social.domain.GuildWaitMember> findMyPending(@Param("playerId") Long playerId, @Param("type") online.lifeasgame.social.domain.GuildWaitType type, Pageable pageable);

    @Query(
        """
            SELECT g.id
            FROM Guild g
            WHERE (:keyword IS NULL OR :keyword='' OR LOWER(g.name.value) LIKE LOWER(CONCAT('%',:keyword,'%') )
                OR LOWER(g.code.value) LIKE LOWER(CONCAT('%',:keyword,'%') ) )
                AND g.visibility = online.lifeasgame.social.domain.GuildVisibility.PUBLIC AND g.status = online.lifeasgame.social.domain.GuildStatus.ACTIVE
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

    @Query("SELECT g.id FROM Guild g WHERE g.visibility = online.lifeasgame.social.domain.GuildVisibility.PUBLIC AND g.status = online.lifeasgame.social.domain.GuildStatus.ACTIVE ORDER BY g.createdAt DESC, g.id DESC")
    List<Long> findRecent(Pageable pageable);
}
