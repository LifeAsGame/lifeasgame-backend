package online.lifeasgame.person.domain.repository;

import online.lifeasgame.person.domain.Person;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PersonRepository {
    Person save(Person person);

    Optional<Person> findByIdAndOwnerPlayerId(Long id, Long ownerPlayerId);

    Optional<Person> findOwnedForUpdate(Long id, Long ownerPlayerId);

    Optional<Person> findByOwnerPlayerIdAndLinkedUserId(Long ownerPlayerId, Long linkedUserId);

    List<Person> findAllByIdInAndOwnerPlayerId(
            Set<Long> ids,
            Long ownerPlayerId
    );
}
