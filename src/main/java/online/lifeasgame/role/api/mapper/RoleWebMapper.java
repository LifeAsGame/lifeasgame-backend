package online.lifeasgame.role.api.mapper;

import online.lifeasgame.role.api.request.RoleRequest;
import online.lifeasgame.role.api.response.RoleResponse;
import online.lifeasgame.role.application.command.RoleCommand;
import online.lifeasgame.role.application.result.RoleResult;

public final class RoleWebMapper {

    private RoleWebMapper() {
    }

    public static RoleCommand.Create toCreateCommand(RoleRequest.Create request) {
        return new RoleCommand.Create(
                request.roleType(),
                request.name(),
                request.description()
        );
    }

    public static RoleCommand.Update toUpdateCommand(RoleRequest.Update request) {
        return new RoleCommand.Update(
                request.roleType(),
                request.name(),
                request.description()
        );
    }

    public static RoleResponse.Detail toDetail(RoleResult.Detail result) {
        return new RoleResponse.Detail(
                result.id(),
                result.playerId(),
                result.roleType(),
                result.name(),
                result.description(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.version()
        );
    }
}
