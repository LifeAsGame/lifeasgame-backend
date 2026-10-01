package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyMember;
import online.lifeasgame.social.domain.PartyWaitMember;
import online.lifeasgame.social.domain.PartyWaitType;
import online.lifeasgame.social.domain.PartyVisibility;
import online.lifeasgame.social.domain.repository.PartyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class PartyRepositoryAdapter implements PartyRepository {

    private final PartyJpaRepository jpaRepository;

    @Override
    public Party save(Party party) {
        return jpaRepository.save(party);
    }

    @Override
    public Optional<Party> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Party> findByIdAndPlayerId(Long id, Long playerId) {
        return jpaRepository.findByIdAndPlayerId(id, playerId);
    }

    @Override
    public List<Party> search(String keyword, PartyVisibility visibility, int page, int size) {
        Page<Long> idPage = jpaRepository.searchIds(
                keyword,
                visibility,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        if (idPage.isEmpty()) return List.of();
        List<Long> ids = idPage.getContent();
        List<Party> list = jpaRepository.fetchWithTagsByIds(ids);

        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            order.put(ids.get(i), i);
        }
        list.sort(Comparator.comparingInt(party -> order.getOrDefault(party.getId(), Integer.MAX_VALUE)));
        return list;
    }

    @Override
    public long countSearch(String keyword, PartyVisibility visibility) {
        return jpaRepository.searchIds(keyword, visibility, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<Party> recent(int limit) {
        List<Long> ids = jpaRepository.findRecent(PageRequest.of(0, limit));
        if (ids.isEmpty()) return List.of();
        List<Party> rows = jpaRepository.findRecentWithTags(ids);
        rows.sort(Comparator.comparingInt(party -> ids.indexOf(party.getId())));
        return rows;
    }
    @Override
    public Page<Party> findMine(Long playerId, Pageable pageable) {
        return jpaRepository.findMine(playerId, pageable);
    }

    @Override
    public Page<PartyMember> findMembers(Long partyId, Pageable pageable) {
        return jpaRepository.findMembers(partyId, pageable);
    }

    @Override
    public Page<PartyWaitMember> findPendingRequests(Long partyId, Pageable pageable) {
        return jpaRepository.findPendingRequests(partyId, pageable);
    }

    @Override
    public Page<PartyWaitMember> findMyPending(Long playerId, PartyWaitType type, Pageable pageable) {
        return jpaRepository.findMyPending(playerId, type, pageable);
    }
}
