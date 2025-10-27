package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.query.GuildQueryRepository;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildVisibility;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.GuildRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class GuildReader {

    private final GuildRepository guildRepository;
    private final GuildQueryRepository guildQueryRepository;

    public Guild get(Long guildId) {
        return guildRepository.findById(guildId)
                .orElseThrow(() -> new DomainException(SocialError.GUILD_NOT_FOUND));
    }

    public Guild getOwned(Long playerId, Long id) {
        return guildRepository.findByIdAndPlayerId(id, playerId)
                .orElseThrow(() -> new DomainException(SocialError.GUILD_NOT_FOUND));
    }

    public List<Guild> search(String keyword, String visibility, int page, int size) {
        GuildVisibility vis = parseVisibility(visibility);
        return guildQueryRepository.search(keyword, vis, page, size);
    }

    public long countSearch(String keyword, String visibility) {
        GuildVisibility vis = parseVisibility(visibility);
        return guildQueryRepository.countSearch(keyword, vis);
    }

    private GuildVisibility parseVisibility(String visibility) {
        return (visibility == null || visibility.isBlank()) ? null : GuildVisibility.valueOf(visibility);
    }

    public List<Guild> recent(int limit) {
        return guildQueryRepository.recent(limit);
    }

    public Guild getGuild(Long playerId, Long guildId) {
        return guildRepository.findByIdAndPlayerId(playerId, guildId)
                .orElseThrow(() -> new DomainException(SocialError.GUILD_NOT_FOUND));
    }
}
