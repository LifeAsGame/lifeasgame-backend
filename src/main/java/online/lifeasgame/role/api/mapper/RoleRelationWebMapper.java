package online.lifeasgame.role.api.mapper;

import online.lifeasgame.role.api.request.RoleRelationRequest;
import online.lifeasgame.role.api.response.RoleRelationResponse;
import online.lifeasgame.role.application.command.RoleRelationCommand;
import online.lifeasgame.role.application.result.RoleRelationResult;

public final class RoleRelationWebMapper {

    private RoleRelationWebMapper() {
    }

    public static RoleRelationCommand.Create toCreateCommand(
            RoleRelationRequest.Create request
    ) {
        return new RoleRelationCommand.Create(
                request.personId(),
                request.relationType(),
                request.roleNotes()
        );
    }

    public static RoleRelationCommand.Update toUpdateCommand(
            RoleRelationRequest.Update request
    ) {
        return new RoleRelationCommand.Update(
                request.relationType(),
                request.roleNotes()
        );
    }

    public static RoleRelationResponse.Detail toDetail(
            RoleRelationResult.Detail result
    ) {
        return new RoleRelationResponse.Detail(
                result.id(),
                result.personId(),
                result.personDisplayName(),
                result.linkedUserId(),
                result.relationType(),
                result.roleNotes(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.version()
        );
    }
}
