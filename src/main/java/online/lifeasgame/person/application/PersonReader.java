package online.lifeasgame.person.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.PersonStatus;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.person.domain.repository.PersonRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PersonReader {

    private final PersonRepository repository;

    Person getOwned(Long personId, Long ownerPlayerId) {
        return repository.findByIdAndOwnerPlayerId(personId, ownerPlayerId)
                .orElseThrow(() -> new DomainException(PersonError.PERSON_NOT_FOUND));
    }

    List<Person> findActive(Long ownerPlayerId) {
        return repository.findAllByOwnerPlayerIdAndStatus(
                ownerPlayerId,
                PersonStatus.ACTIVE
        );
    }
}
