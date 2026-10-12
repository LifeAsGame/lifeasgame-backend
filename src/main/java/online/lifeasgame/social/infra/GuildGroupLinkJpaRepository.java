package online.lifeasgame.social.infra;

import jakarta.persistence.LockModeType;
import online.lifeasgame.social.domain.GuildGroupLink;
import online.lifeasgame.social.domain.repository.GuildGroupLinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuildGroupLinkJpaRepository extends JpaRepository<GuildGroupLink, Long>, GuildGroupLinkRepository {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM GuildGroupLink l WHERE l.id = :id")
    Optional<GuildGroupLink> findForUpdate(@Param("id") Long id);

    @Override
    @Query("SELECT l FROM GuildGroupLink l WHERE l.guildId = :guildId AND l.groupType = :type AND l.groupId = :groupId AND l.status IN (online.lifeasgame.social.domain.GuildGroupLink.Status.PENDING, online.lifeasgame.social.domain.GuildGroupLink.Status.ACTIVE)")
    Optional<GuildGroupLink> findOpen(@Param("guildId") Long guildId, @Param("type") GuildGroupLink.GroupType type, @Param("groupId") Long groupId);

    @Override
    Page<GuildGroupLink> findByGuildIdAndStatusOrderByIdDesc(Long guildId, GuildGroupLink.Status status, Pageable page);

    @Override
    @Query(value = """
            SELECT l.* FROM guild_group_links l
            LEFT JOIN parties p ON l.group_type = 'PARTY' AND p.party_id = l.group_id AND p.status = 'ACTIVE'
            LEFT JOIN role_parties rp ON l.group_type = 'ROLE_PARTY' AND rp.id = l.group_id AND rp.status = 'ACTIVE'
            WHERE l.guild_id = :guildId AND l.status = 'PENDING'
              AND (:guildLeader = true OR l.proposed_by_player_id = :actor
                   OR p.leader_player_id = :actor OR rp.leader_player_id = :actor)
            ORDER BY l.id DESC
            """, countQuery = """
            SELECT COUNT(*) FROM guild_group_links l
            LEFT JOIN parties p ON l.group_type = 'PARTY' AND p.party_id = l.group_id AND p.status = 'ACTIVE'
            LEFT JOIN role_parties rp ON l.group_type = 'ROLE_PARTY' AND rp.id = l.group_id AND rp.status = 'ACTIVE'
            WHERE l.guild_id = :guildId AND l.status = 'PENDING'
              AND (:guildLeader = true OR l.proposed_by_player_id = :actor
                   OR p.leader_player_id = :actor OR rp.leader_player_id = :actor)
            """, nativeQuery = true)
    Page<GuildGroupLink> findVisiblePending(@Param("guildId") Long guildId, @Param("actor") Long actor,
                                            @Param("guildLeader") boolean guildLeader, Pageable page);

    @Override
    @Query(value = """
            SELECT l.id AS linkId,
                   CASE WHEN l.group_type = 'PARTY' THEN p.leader_player_id ELSE rp.leader_player_id END AS leaderId,
                   CASE WHEN l.group_type = 'PARTY' THEN COALESCE(p.status = 'ACTIVE', 0)
                        ELSE COALESCE(rp.status = 'ACTIVE', 0) END AS active,
                   CASE WHEN l.group_type = 'PARTY' THEN COALESCE(p.visibility = 'PUBLIC', 0)
                        ELSE 0 END AS publicPreview,
                   CASE WHEN l.group_type = 'PARTY' THEN EXISTS (
                       SELECT 1 FROM party_members pm WHERE pm.party_id = l.group_id AND pm.player_id = :actor)
                        ELSE EXISTS (SELECT 1 FROM role_party_members rpm
                            WHERE rpm.role_party_id = l.group_id AND rpm.player_id = :actor AND rpm.left_at IS NULL)
                   END AS member
            FROM guild_group_links l
            LEFT JOIN parties p ON l.group_type = 'PARTY' AND p.party_id = l.group_id
            LEFT JOIN role_parties rp ON l.group_type = 'ROLE_PARTY' AND rp.id = l.group_id
            WHERE l.id IN :ids
            """, nativeQuery = true)
    List<LinkTarget> findTargets(@Param("ids") List<Long> ids, @Param("actor") Long actor);
}
