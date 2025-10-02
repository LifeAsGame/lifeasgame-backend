package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminPlayerHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerHobbyResponse;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;

public class AdminPlayerHobbyWebMapper {

    private AdminPlayerHobbyWebMapper() {}

    public static AdminPlayerHobbyResponse.Granted toGrantedHobby(
            PlayerHobbyResult.Granted result
    ) {
        return AdminPlayerHobbyResponse.Granted.of(
                result.playerId(),
                result.hobbyId(),
                result.name(),
                result.category(),
                result.customName(),
                result.detail(),
                result.proficiency(),
                result.status(),
                result.startedOn(),
                result.xp()
        );
    }

    public static PlayerHobbyCommand.Grant toCommand(
            Long playerId,
            Long hobbyId,
            AdminPlayerHobbyRequest.Grant request
    ) {
        return PlayerHobbyCommand.Grant.of(
                playerId,
                hobbyId,
                request.customName(),
                request.detail(),
                request.proficiency(),
                request.status(),
                request.startedOn()
        );
    }
}
