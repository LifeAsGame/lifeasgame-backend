package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.Guild;

import java.util.Optional;

public interface GuildRepository {
    Guild save(Guild g);

    Optional<Guild> findById(Long id);

    Optional<Guild> findByIdAndPlayerId(Long id, Long playerId);
}
