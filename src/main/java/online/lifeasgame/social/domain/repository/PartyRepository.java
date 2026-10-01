package online.lifeasgame.social.domain.repository;

import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyVisibility;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import online.lifeasgame.social.domain.PartyMember;
import online.lifeasgame.social.domain.PartyWaitMember;
import online.lifeasgame.social.domain.PartyWaitType;

import java.util.List;
import java.util.Optional;

public interface PartyRepository {
    Party save(Party party);

    Optional<Party> findById(Long id);

    Optional<Party> findByIdAndPlayerId(Long id, Long playerId);

    List<Party> search(String keyword, PartyVisibility visibility, int page, int size);

    long countSearch(String keyword, PartyVisibility visibility);

    List<Party> recent(int limit);

    Page<Party> findMine(Long playerId, Pageable pageable);
    Page<PartyMember> findMembers(Long partyId, Pageable pageable);
    Page<PartyWaitMember> findPendingRequests(Long partyId, Pageable pageable);
    Page<PartyWaitMember> findMyPending(Long playerId, PartyWaitType type, Pageable pageable);
}
