package online.lifeasgame.role.api.mapper;

import online.lifeasgame.role.api.request.RoleEventRequest;
import online.lifeasgame.role.api.response.RoleEventResponse;
import online.lifeasgame.role.application.command.RoleEventCommand;
import online.lifeasgame.role.application.result.RoleEventResult;

public final class RoleEventWebMapper {

    private RoleEventWebMapper() {
    }

    public static RoleEventCommand.Create toCreateCommand(
            RoleEventRequest.Create request
    ) {
        return new RoleEventCommand.Create(
                request.title(),
                request.description(),
                request.startsAt(),
                request.endsAt()
        );
    }

    public static RoleEventCommand.Update toUpdateCommand(
            RoleEventRequest.Update request
    ) {
        return new RoleEventCommand.Update(
                request.title(),
                request.description(),
                request.startsAt(),
                request.endsAt()
        );
    }

    public static RoleEventCommand.AddParticipant toAddParticipantCommand(
            RoleEventRequest.AddParticipant request
    ) {
        return new RoleEventCommand.AddParticipant(
                request.participantType(),
                request.participantId()
        );
    }

    public static RoleEventResponse.Detail toDetail(
            RoleEventResult.Detail result
    ) {
        return new RoleEventResponse.Detail(
                result.id(),
                result.roleId(),
                result.title(),
                result.description(),
                result.startsAt(),
                result.endsAt(),
                result.status(),
                result.completedAt(),
                result.participants().stream()
                        .map(RoleEventWebMapper::toParticipant)
                        .toList(),
                result.createdAt(),
                result.updatedAt(),
                result.version()
        );
    }

    public static RoleEventResponse.Participant toParticipant(
            RoleEventResult.Participant result
    ) {
        return new RoleEventResponse.Participant(
                result.participantLinkId(),
                result.participantType(),
                result.participantId()
        );
    }
}
