package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
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

    private final PartyRepository repository;

    public Party getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new DomainException(SocialError.PARTY_NOT_FOUND));
    }

    public Party getByPlayerIdAndId(Long playerId, Long id) {
        return repository.findByIdAndPlayerId(id, playerId)
                .orElseThrow(() -> new DomainException(SocialError.PARTY_NOT_FOUND));
    }

    public List<Party> search(String keyword, String visibility, int page, int size) {
        PartyVisibility partyVisibility = parseVisibility(visibility);
        return repository.search(keyword, partyVisibility, page, size);
    }

    public long countSearch(String keyword, String visibility) {
        PartyVisibility partyVisibility = parseVisibility(visibility);
        return repository.countSearch(keyword, partyVisibility);
    }

    private PartyVisibility parseVisibility(String visibility) {
        return (visibility == null || visibility.isBlank()) ? null : PartyVisibility.valueOf(visibility);
    }

    public List<Party> recent(int limit) {
        return repository.recent(limit);
    }
}
