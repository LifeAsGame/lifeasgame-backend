package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminHobbyResponse;
import online.lifeasgame.character.application.command.HobbyCommand;
import online.lifeasgame.character.application.result.HobbyResult;

public final class AdminHobbyWebMapper {

    private AdminHobbyWebMapper() {}

    public static HobbyCommand.Create toCreateCommand(AdminHobbyRequest.Create request) {
        return new HobbyCommand.Create(
                request.name(),
                request.category()
        );
    }

    public static AdminHobbyResponse.Info toInfo(HobbyResult.Info result) {
        return new AdminHobbyResponse.Info(
                result.hobbyId(),
                result.name(),
                result.category()
        );
    }
}
