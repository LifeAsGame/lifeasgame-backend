package online.lifeasgame.person.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.query.PersonQuery;
import online.lifeasgame.person.application.result.PersonResult;
import online.lifeasgame.person.domain.error.PersonError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonQueryService {

    private final PersonQuery query;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public List<PersonResult.Detail> list() {
        return query.findActive(currentPlayerAccessor.currentPlayerIdOrThrow());
    }

    public PersonResult.Detail detail(Long personId) {
        Long ownerPlayerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return query.findOwned(personId, ownerPlayerId)
                .orElseThrow(() -> new DomainException(PersonError.PERSON_NOT_FOUND));
    }
}
