package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminPlayerHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerHobbyResponse;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;

import java.util.List;

public final class AdminPlayerHobbyWebMapper {

    private AdminPlayerHobbyWebMapper() {}

    public static AdminPlayerHobbyResponse.Infos toInfos(
            Long playerId,
            List<PlayerHobbyResult.Info> results
    ) {
        return new AdminPlayerHobbyResponse.Infos(
                playerId,
                results.stream()
                        .map(result -> new AdminPlayerHobbyResponse.Info(
                                result.hobbyId(),
                                result.name(),
                                result.category(),
                                result.proficiency(),
                                result.status(),
                                result.startedOn(),
                                result.xp()
                        ))
                        .toList()
        );
    }

    public static PlayerHobbyCommand.Create toCreatedCommand(
            Long hobbyId,
            AdminPlayerHobbyRequest.Grant request
    ) {
        return new PlayerHobbyCommand.Create(
                hobbyId,
                request.customName(),
                request.detail(),
                request.proficiency(),
                request.status(),
                request.startedOn()
        );
    }

    public static AdminPlayerHobbyResponse.Granted toGranted(PlayerHobbyResult.Created result) {
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

    public static AdminPlayerHobbyResponse.Revoked toRevoked(PlayerHobbyResult.Revoked result) {
        return new AdminPlayerHobbyResponse.Revoked(
                result.playerId(),
                result.hobbyId()
        );
    }
}
