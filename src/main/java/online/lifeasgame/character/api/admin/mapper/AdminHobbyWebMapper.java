package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminHobbyResponse;
import online.lifeasgame.character.application.command.HobbyCommand;
import online.lifeasgame.character.application.result.HobbyResult;

public class AdminHobbyWebMapper {

    public static HobbyCommand.Create toCommand(AdminHobbyRequest.CreateHobby request) {
        return HobbyCommand.Create.of(
                request.name(),
                request.category()
        );
    }

    public static AdminHobbyResponse.HobbyInfo toHobbyInfo(
            HobbyResult.HobbyInfo result
    ) {
        return AdminHobbyResponse.HobbyInfo.of(
                result.hobbyId(),
                result.name(),
                result.category()
        );
    }
}
