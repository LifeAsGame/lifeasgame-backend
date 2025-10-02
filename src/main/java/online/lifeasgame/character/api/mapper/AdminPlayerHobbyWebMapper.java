package online.lifeasgame.character.api.mapper;

import online.lifeasgame.character.application.command.AdminPlayerHobbyCommand;
import online.lifeasgame.character.application.result.AdminPlayerHobbyResult;
import online.lifeasgame.character.api.request.AdminPlayerHobbyRequest;
import online.lifeasgame.character.api.response.AdminPlayerHobbyResponse;

public class AdminPlayerHobbyWebMapper {

    private AdminPlayerHobbyWebMapper() {}

    public static AdminPlayerHobbyResponse.GrantedHobby toGrantedHobby(
            AdminPlayerHobbyResult.GrantedHobby result
    ) {
        return AdminPlayerHobbyResponse.GrantedHobby.of(
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

    public static AdminPlayerHobbyCommand.GrantHobby toCommand(
            Long playerId,
            Long hobbyId,
            AdminPlayerHobbyRequest.GrantHobby request
    ) {
        return AdminPlayerHobbyCommand.GrantHobby.of(
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
