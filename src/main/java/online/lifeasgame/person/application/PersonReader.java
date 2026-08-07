package online.lifeasgame.person.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.error.PersonError;
import online.lifeasgame.person.domain.repository.PersonRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class PersonReader {

    private final PersonRepository repository;

    Person getOwned(Long personId, Long ownerPlayerId) {
        return repository.findByIdAndOwnerPlayerId(personId, ownerPlayerId)
                .orElseThrow(() -> new DomainException(PersonError.PERSON_NOT_FOUND));
    }

    List<Person> findOwnedByIds(Set<Long> personIds, Long ownerPlayerId) {
        return repository.findAllByIdInAndOwnerPlayerId(personIds, ownerPlayerId);
    }
}
