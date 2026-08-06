package online.lifeasgame.person.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.PersonStatus;
import online.lifeasgame.person.domain.repository.PersonRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    public List<Person> findAllByOwnerPlayerIdAndStatus(
            Long ownerPlayerId,
            PersonStatus status
    ) {
        return repository.findAllByOwnerPlayerIdAndStatusOrderByIdAsc(
                ownerPlayerId,
                status
        );
    }
}
