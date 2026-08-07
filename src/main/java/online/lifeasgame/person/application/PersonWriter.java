package online.lifeasgame.person.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.person.domain.Person;
import online.lifeasgame.person.domain.repository.PersonRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class PersonWriter {

    private final PersonRepository repository;

    Person save(Person person) {
        return repository.save(person);
    }
}
