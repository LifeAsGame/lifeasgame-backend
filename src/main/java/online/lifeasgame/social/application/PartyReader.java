package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.query.PartyQueryRepository;
import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyVisibility;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.PartyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PartyReader {

    private final PartyRepository partyRepository;
    private final PartyQueryRepository partyQueryRepository;

    public Party get(Long id) {
        return partyRepository.findById(id).orElseThrow(() -> new DomainException(SocialError.PARTY_NOT_FOUND));
    }

    public Party getOwned(Long playerId, Long id) {
        return partyRepository.findByIdAndPlayerId(
                id,
                playerId
        ).orElseThrow(() -> new DomainException(SocialError.PARTY_NOT_FOUND));
    }

    public List<Party> search(String keyword, String visibility, int page, int size) {
        PartyVisibility vis = parseVisibility(visibility);
        return partyQueryRepository.search(keyword, vis, page, size);
    }

    public long countSearch(String keyword, String visibility) {
        PartyVisibility vis = parseVisibility(visibility);
        return partyQueryRepository.countSearch(keyword, vis);
    }

    private PartyVisibility parseVisibility(String visibility) {
        return (visibility == null || visibility.isBlank()) ? null : PartyVisibility.valueOf(visibility);
    }

    public List<Party> recent(int limit) {
        return partyQueryRepository.recent(limit);
    }

    public Party getParty(Long playerId, Long id) {
        return partyRepository.findByIdAndPlayerId(
                id,
                playerId
        ).orElseThrow(() -> new DomainException(SocialError.PARTY_NOT_FOUND));
    }
}
