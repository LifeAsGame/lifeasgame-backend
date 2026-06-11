package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
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

    private final GuildRepository repository;

    public Guild getByIdOrThrow(Long guildId) {
        return repository.findById(guildId)
                .orElseThrow(() -> new DomainException(SocialError.GUILD_NOT_FOUND));
    }

    public Guild getByPlayerIdAndIdOrThrow(Long playerId, Long id) {
        return repository.findByIdAndPlayerId(id, playerId)
                .orElseThrow(() -> new DomainException(SocialError.GUILD_NOT_FOUND));
    }

    public List<Guild> search(String keyword, String visibility, int page, int size) {
        GuildVisibility guildVisibility = parseVisibility(visibility);
        return repository.search(keyword, guildVisibility, page, size);
    }

    public long countSearch(String keyword, String visibility) {
        GuildVisibility guildVisibility = parseVisibility(visibility);
        return repository.countSearch(keyword, guildVisibility);
    }

    private GuildVisibility parseVisibility(String visibility) {
        return (visibility == null || visibility.isBlank()) ? null : GuildVisibility.valueOf(visibility);
    }

    public List<Guild> recent(int limit) {
        return repository.recent(limit);
    }
}
