package online.lifeasgame.person.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.person.application.internal.PersonLookupApi;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.PersonStatus;
import online.lifeasgame.person.domain.error.PersonError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PersonLookupService implements PersonLookupApi {

    private final PersonReader reader;

    @Override
    public PersonReference getOwnedActive(Long personId, Long ownerPlayerId) {
        Person person = reader.getOwned(personId, ownerPlayerId);
        if (person.getStatus() == PersonStatus.ARCHIVED) {
            throw new DomainException(PersonError.PERSON_ARCHIVED);
        }
        return reference(person);
    }

    @Override
    public Map<Long, PersonReference> findOwnedByIds(
            Set<Long> personIds,
            Long ownerPlayerId
    ) {
        if (personIds.isEmpty()) {
            return Map.of();
        }
        return reader.findOwnedByIds(personIds, ownerPlayerId).stream()
                .map(PersonLookupService::reference)
                .collect(Collectors.toUnmodifiableMap(
                        PersonReference::id,
                        Function.identity()
                ));
    }

    private static PersonReference reference(Person person) {
        return new PersonReference(
                person.getId(),
                person.getLinkedUserId(),
                person.getDisplayName()
        );
    }
}
