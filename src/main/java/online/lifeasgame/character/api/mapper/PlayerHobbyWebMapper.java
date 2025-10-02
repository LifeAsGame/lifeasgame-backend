package online.lifeasgame.character.api.mapper;

import java.util.List;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.api.request.PlayerHobbyRequest;
import online.lifeasgame.character.api.response.PlayerHobbyResponse;

public class PlayerHobbyWebMapper {

    private PlayerHobbyWebMapper() {}

    public static PlayerHobbyResponse.PlayerHobbyInfos toPlayerHobbyInfos(List<PlayerHobbyResult.PlayerHobbyInfo> playerHobbyInfos) {
        return PlayerHobbyResponse.PlayerHobbyInfos.of(
                playerHobbyInfos.stream()
                        .map(
                                playerHobbyInfo ->
                                        PlayerHobbyResponse.PlayerHobbyInfo.of(
                                                playerHobbyInfo.hobbyId(),
                                                playerHobbyInfo.name(),
                                                playerHobbyInfo.category(),
                                                playerHobbyInfo.customName(),
                                                playerHobbyInfo.detail(),
                                                playerHobbyInfo.proficiency(),
                                                playerHobbyInfo.status(),
                                                playerHobbyInfo.startedOn(),
                                                playerHobbyInfo.xp()
                                        )
                        )
                        .toList()
        );
    }

    public static PlayerHobbyCommand.ChangePlayerHobby toCommand(
            Long hobbyId,
            PlayerHobbyRequest.ChangePlayerHobby request
    ) {
        return PlayerHobbyCommand.ChangePlayerHobby.of(
                hobbyId,
                request.customName(),
                request.detail(),
                request.proficiency(),
                request.status(),
                request.startedOn()
        );
    }

    public static PlayerHobbyCommand.CreatePlayerHobby toCommand(
            Long hobbyId,
            PlayerHobbyRequest.CreatePlayerHobby request
    ) {
        return PlayerHobbyCommand.CreatePlayerHobby.of(
                hobbyId,
                request.customName(),
                request.detail(),
                request.proficiency(),
                request.status(),
                request.startedOn()
        );
    }

    public static PlayerHobbyResponse.CreatedPlayerHobby toCreatedPlayerHobby(
            PlayerHobbyResult.CreatedPlayerHobby result
    ) {
        return PlayerHobbyResponse.CreatedPlayerHobby.of(
                result.hobbyId(),
                result.customName(),
                result.detail(),
                result.proficiency(),
                result.status(),
                result.startedOn(),
                result.xp()
        );
    }

    public static PlayerHobbyResponse.ChangedPlayerHobby toChangedPlayerHobby(
            PlayerHobbyResult.ChangedPlayerHobby result
    ) {
        return PlayerHobbyResponse.ChangedPlayerHobby.of(
                result.hobbyId(),
                result.customName(),
                result.detail(),
                result.proficiency(),
                result.status(),
                result.startedOn(),
                result.xp()
        );
    }
}
