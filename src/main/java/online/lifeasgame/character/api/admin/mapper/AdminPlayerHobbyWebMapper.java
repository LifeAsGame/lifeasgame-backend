package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminPlayerHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerHobbyResponse;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;

public final class AdminPlayerHobbyWebMapper {

    private AdminPlayerHobbyWebMapper() {}

    public static PlayerHobbyCommand.Grant toGrantCommand(
            Long playerId,
            Long hobbyId,
            AdminPlayerHobbyRequest.Grant request
    ) {
        return new PlayerHobbyCommand.Grant(
                playerId,
                hobbyId,
                request.customName(),
                request.detail(),
                request.proficiency(),
                request.status(),
                request.startedOn()
        );
    }

    public static AdminPlayerHobbyResponse.Granted toGranted(PlayerHobbyResult.Granted result) {
        return new AdminPlayerHobbyResponse.Granted(
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
}
