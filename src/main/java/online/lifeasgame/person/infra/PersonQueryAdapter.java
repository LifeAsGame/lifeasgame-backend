package online.lifeasgame.person.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.person.application.query.PersonQuery;
import online.lifeasgame.person.application.result.PersonResult;
import online.lifeasgame.person.domain.PersonStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PersonQueryAdapter implements PersonQuery {

    private final JpaPersonRepository repository;

    @Override
    public List<PersonResult.Detail> findActive(Long ownerPlayerId) {
        return repository.findAllByOwnerPlayerIdAndStatusOrderByIdAsc(
                        ownerPlayerId,
                        PersonStatus.ACTIVE
                ).stream()
                .map(PersonResult.Detail::from)
                .toList();
    }

    @Override
    public Optional<PersonResult.Detail> findOwned(
            Long personId,
            Long ownerPlayerId
    ) {
        return repository.findByIdAndOwnerPlayerId(personId, ownerPlayerId)
                .map(PersonResult.Detail::from);
    }
}
