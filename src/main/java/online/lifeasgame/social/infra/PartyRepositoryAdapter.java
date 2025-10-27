package online.lifeasgame.social.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.social.application.query.PartyQueryRepository;
import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyVisibility;
import online.lifeasgame.social.domain.repository.PartyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class PartyRepositoryAdapter implements PartyRepository, PartyQueryRepository {

    private final PartyJpaRepository partyJpaRepository;

    @Override
    public Party save(Party party) {
        return partyJpaRepository.save(party);
    }

    @Override
    public Optional<Party> findById(Long id) {
        return partyJpaRepository.findById(id);
    }

    @Override
    public Optional<Party> findByIdAndPlayerId(Long id, Long playerId) {
        return partyJpaRepository.findByIdAndPlayerId(id, playerId);
    }

    @Override
    public List<Party> search(String keyword, PartyVisibility visibility, int page, int size) {
        Page<Long> idPage = partyJpaRepository.searchIds(
                keyword,
                visibility,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        if (idPage.isEmpty()) return List.of();
        List<Long> ids = idPage.getContent();
        List<Party> list = partyJpaRepository.fetchWithTagsByIds(ids);

        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            order.put(ids.get(i), i);
        }
        list.sort(Comparator.comparingInt(party -> order.getOrDefault(party.getId(), Integer.MAX_VALUE)));
        return list;
    }

    @Override
    public long countSearch(String keyword, PartyVisibility visibility) {
        return partyJpaRepository.searchIds(keyword, visibility, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<Party> recent(int limit) {
        List<Long> ids = partyJpaRepository.findRecent(limit);
        return partyJpaRepository.findRecentWithTags(ids);
    }
}
