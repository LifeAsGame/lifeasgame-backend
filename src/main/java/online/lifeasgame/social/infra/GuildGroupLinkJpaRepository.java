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
    java.util.List<GuildGroupLink> findByGuildIdAndStatusOrderByIdDesc(Long guildId, GuildGroupLink.Status status);
}
