package online.lifeasgame.person.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.repository.PersonRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class PersonRepositoryAdapter implements PersonRepository {

    private final JpaPersonRepository repository;

    @Override
    public Person save(Person person) {
        return repository.save(person);
    }

    @Override
    public Optional<Person> findByIdAndOwnerPlayerId(Long id, Long ownerPlayerId) {
        return repository.findByIdAndOwnerPlayerId(id, ownerPlayerId);
    }

    @Override
    public List<Person> findAllByIdInAndOwnerPlayerId(
            Set<Long> ids,
            Long ownerPlayerId
    ) {
        return repository.findAllByIdInAndOwnerPlayerId(ids, ownerPlayerId);
    }
}
