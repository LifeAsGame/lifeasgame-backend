package online.lifeasgame.person.application.query;

import online.lifeasgame.person.application.result.PersonResult;

import java.util.List;
import java.util.Optional;

public interface PersonQuery {
    List<PersonResult.Detail> findActive(Long ownerPlayerId);

    Optional<PersonResult.Detail> findOwned(Long personId, Long ownerPlayerId);
}
