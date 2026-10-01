package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.GuildGroupLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface GuildGroupLinkRepository {
    GuildGroupLink saveAndFlush(GuildGroupLink link);
    Optional<GuildGroupLink> findForUpdate(Long id);
    Optional<GuildGroupLink> findOpen(Long guildId, GuildGroupLink.GroupType type, Long groupId);
    Page<GuildGroupLink> findByGuildIdAndStatusOrderByIdDesc(Long guildId, GuildGroupLink.Status status, Pageable page);
    List<GuildGroupLink> findByGuildIdAndStatusOrderByIdDesc(Long guildId, GuildGroupLink.Status status);
}
