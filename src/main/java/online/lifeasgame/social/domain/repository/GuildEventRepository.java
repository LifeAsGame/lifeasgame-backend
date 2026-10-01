package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.GuildEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface GuildEventRepository {
    GuildEvent saveAndFlush(GuildEvent event);
    Optional<GuildEvent> findByIdAndGuildId(Long id, Long guildId);
    Optional<GuildEvent> findForUpdate(Long id, Long guildId);
    Page<GuildEvent> findByGuildIdOrderByIdDesc(Long guildId, Pageable page);
}
