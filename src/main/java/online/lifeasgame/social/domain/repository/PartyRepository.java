package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyVisibility;

import java.util.List;
import java.util.Optional;

public interface PartyRepository {
    Party save(Party party);

    Optional<Party> findById(Long id);

    Optional<Party> findByIdAndPlayerId(Long id, Long playerId);

    List<Party> search(String keyword, PartyVisibility visibility, int page, int size);

    long countSearch(String keyword, PartyVisibility visibility);

    List<Party> recent(int limit);
}
