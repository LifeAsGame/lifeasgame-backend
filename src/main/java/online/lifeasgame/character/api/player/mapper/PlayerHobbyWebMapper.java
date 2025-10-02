package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;
import online.lifeasgame.character.api.player.request.PlayerHobbyRequest;
import online.lifeasgame.character.api.player.response.PlayerHobbyResponse;

public class PlayerHobbyWebMapper {

    private PlayerHobbyWebMapper() {}

    public static PlayerHobbyResponse.Infos toPlayerHobbyInfos(List<PlayerHobbyResult.Info> infos) {
        return PlayerHobbyResponse.Infos.of(
                infos.stream()
                        .map(
                                playerHobbyInfo ->
                                        PlayerHobbyResponse.Info.of(
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

    public static PlayerHobbyCommand.Change toCommand(
            Long hobbyId,
            PlayerHobbyRequest.Change request
    ) {
        return PlayerHobbyCommand.Change.of(
                hobbyId,
                request.customName(),
                request.detail(),
                request.proficiency(),
                request.status(),
                request.startedOn()
        );
    }

    public static PlayerHobbyCommand.Create toCommand(
            Long hobbyId,
            PlayerHobbyRequest.Create request
    ) {
        return PlayerHobbyCommand.Create.of(
                hobbyId,
                request.customName(),
                request.detail(),
                request.proficiency(),
                request.status(),
                request.startedOn()
        );
    }

    public static PlayerHobbyResponse.Created toCreatedPlayerHobby(
            PlayerHobbyResult.Created result
    ) {
        return PlayerHobbyResponse.Created.of(
                result.hobbyId(),
                result.customName(),
                result.detail(),
                result.proficiency(),
                result.status(),
                result.startedOn(),
                result.xp()
        );
    }

    public static PlayerHobbyResponse.Changed toChangedPlayerHobby(
            PlayerHobbyResult.Changed result
    ) {
        return PlayerHobbyResponse.Changed.of(
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
