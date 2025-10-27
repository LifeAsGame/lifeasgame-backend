package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.Party;

import java.util.Optional;

public interface PartyRepository {
    Party save(Party party);

    Optional<Party> findById(Long id);

    Optional<Party> findByIdAndPlayerId(Long id, Long playerId);
}
