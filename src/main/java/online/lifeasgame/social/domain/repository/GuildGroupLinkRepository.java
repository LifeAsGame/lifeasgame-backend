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
    Page<GuildGroupLink> findVisiblePending(Long guildId, Long actor, boolean guildLeader, Pageable page);
    List<LinkTarget> findTargets(List<Long> ids, Long actor);

    interface LinkTarget {
        Long getLinkId();
        Long getLeaderId();
        Long getActive();
        Long getPublicPreview();
        Long getMember();
    }
}
