package online.lifeasgame.person.api.mapper;

import online.lifeasgame.person.api.request.PersonRequest;
import online.lifeasgame.person.api.response.PersonResponse;
import online.lifeasgame.person.application.command.PersonCommand;
import online.lifeasgame.person.application.result.PersonResult;

public final class PersonWebMapper {

    private PersonWebMapper() {
    }

    public static PersonCommand.Create toCreateCommand(PersonRequest.Create request) {
        return new PersonCommand.Create(
                request.displayName(),
                request.notes(),
                request.birthday(),
                request.contact()
        );
    }

    public static PersonCommand.Update toUpdateCommand(PersonRequest.Update request) {
        return new PersonCommand.Update(
                request.displayName(),
                request.notes(),
                request.birthday(),
                request.contact()
        );
    }

    public static PersonResponse.Detail toDetail(PersonResult.Detail result) {
        return new PersonResponse.Detail(
                result.id(),
                result.linkedUserId(),
                result.displayName(),
                result.notes(),
                result.birthday(),
                result.contact(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.version()
        );
    }
}
