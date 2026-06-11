package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildVisibility;

import java.util.List;
import java.util.Optional;

public interface GuildRepository {
    Guild save(Guild g);

    Optional<Guild> findById(Long id);

    Optional<Guild> findByIdAndPlayerId(Long id, Long playerId);

    List<Guild> search(String keyword, GuildVisibility visibility, int page, int size);

    long countSearch(String keyword, GuildVisibility visibility);

    List<Guild> recent(int limit);
}
