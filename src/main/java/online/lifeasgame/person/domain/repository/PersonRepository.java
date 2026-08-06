package online.lifeasgame.person.domain.repository;

import online.lifeasgame.person.domain.Person;
import java.util.Optional;

public interface PersonRepository {
    Person save(Person person);

    Optional<Person> findByIdAndOwnerPlayerId(Long id, Long ownerPlayerId);
}
