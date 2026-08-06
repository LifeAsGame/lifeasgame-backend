package online.lifeasgame.person.domain.repository;

import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.PersonStatus;

import java.util.List;
import java.util.Optional;

public interface PersonRepository {
    Person save(Person person);

    Optional<Person> findByIdAndOwnerPlayerId(Long id, Long ownerPlayerId);

    List<Person> findAllByOwnerPlayerIdAndStatus(
            Long ownerPlayerId,
            PersonStatus status
    );
}
