package online.lifeasgame.social.application.query;

import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyVisibility;

import java.util.List;

public interface PartyQueryRepository {
    List<Party> search(String keyword, PartyVisibility visibility, int page, int size);

    long countSearch(String keyword, PartyVisibility visibility);

    List<Party> recent(int limit);
}
