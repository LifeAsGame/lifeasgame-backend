package online.lifeasgame.character.api.mapper;

import online.lifeasgame.character.application.command.AdminHobbyCommand;
import online.lifeasgame.character.application.result.AdminHobbyResult;
import online.lifeasgame.character.api.request.AdminHobbyRequest;
import online.lifeasgame.character.api.response.AdminHobbyResponse;

public class AdminHobbyWebMapper {

    public static AdminHobbyCommand.CreateHobby toCommand(AdminHobbyRequest.CreateHobby request) {
        return AdminHobbyCommand.CreateHobby.of(
                request.name(),
                request.category()
        );
    }

    public static AdminHobbyResponse.HobbyInfo toHobbyInfo(
            AdminHobbyResult.HobbyInfo result
    ) {
        return AdminHobbyResponse.HobbyInfo.of(
                result.hobbyId(),
                result.name(),
                result.category()
        );
    }
}
