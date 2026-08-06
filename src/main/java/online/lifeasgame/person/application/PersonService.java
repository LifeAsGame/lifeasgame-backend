package online.lifeasgame.person.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.application.result.PersonResult;
import online.lifeasgame.person.domain.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonReader reader;
    private final PersonWriter writer;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public PersonResult.Detail create(PersonCommand.Create command) {
        Long ownerPlayerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Person saved = writer.save(Person.create(
                ownerPlayerId,
                command.displayName(),
                command.notes(),
                command.birthday(),
                command.contact()
        ));
        return PersonResult.Detail.from(saved);
    }

    @Transactional
    public PersonResult.Detail update(
            Long personId,
            PersonCommand.Update command
    ) {
        Long ownerPlayerId = currentPlayerAccessor.currentPlayerIdOrThrow();
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
    public void archive(Long personId) {
        Long ownerPlayerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Person person = reader.getOwned(personId, ownerPlayerId);
        person.archive();
        writer.save(person);
    }
}
