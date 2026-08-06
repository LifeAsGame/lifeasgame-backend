package online.lifeasgame.person.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.application.result.PersonResult;
import online.lifeasgame.person.domain.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonReader reader;
    private final PersonWriter writer;

    @Transactional
    public PersonResult.Detail create(
            Long ownerPlayerId,
            PersonCommand.Create command
    ) {
        Person saved = writer.save(Person.create(
                ownerPlayerId,
                command.displayName(),
                command.notes(),
                command.birthday(),
                command.contact()
        ));
        return PersonResult.Detail.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PersonResult.Detail> list(Long ownerPlayerId) {
        return reader.findActive(ownerPlayerId).stream()
                .map(PersonResult.Detail::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonResult.Detail detail(Long ownerPlayerId, Long personId) {
        return PersonResult.Detail.from(reader.getOwned(personId, ownerPlayerId));
    }

    @Transactional
    public PersonResult.Detail update(
            Long ownerPlayerId,
            Long personId,
            PersonCommand.Update command
    ) {
        Person person = reader.getOwned(personId, ownerPlayerId);
        person.update(
                command.displayName(),
                command.notes(),
                command.birthday(),
                command.contact()
        );
        return PersonResult.Detail.from(writer.save(person));
    }

    @Transactional
    public void archive(Long ownerPlayerId, Long personId) {
        Person person = reader.getOwned(personId, ownerPlayerId);
        person.archive();
        writer.save(person);
    }
}
