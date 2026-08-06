package online.lifeasgame.person.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.application.result.PersonResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PersonFacade {

    private final PersonService service;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public PersonResult.Detail create(PersonCommand.Create command) {
        return service.create(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    public List<PersonResult.Detail> list() {
        return service.list(currentPlayerAccessor.currentPlayerIdOrThrow());
    }

    public PersonResult.Detail detail(Long personId) {
        return service.detail(currentPlayerAccessor.currentPlayerIdOrThrow(), personId);
    }

    public PersonResult.Detail update(Long personId, PersonCommand.Update command) {
        return service.update(
                currentPlayerAccessor.currentPlayerIdOrThrow(),
                personId,
                command
        );
    }

    public void archive(Long personId) {
        service.archive(currentPlayerAccessor.currentPlayerIdOrThrow(), personId);
    }
}
