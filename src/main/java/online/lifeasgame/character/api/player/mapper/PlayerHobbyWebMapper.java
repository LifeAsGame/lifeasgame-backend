package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.request.PlayerHobbyRequest;
import online.lifeasgame.character.api.player.response.PlayerHobbyResponse;
import online.lifeasgame.character.application.command.PlayerHobbyCommand;
import online.lifeasgame.character.application.result.PlayerHobbyResult;

import java.util.List;

public final class PlayerHobbyWebMapper {

    private PlayerHobbyWebMapper() {}

    public static PlayerHobbyResponse.Infos toInfos(List<PlayerHobbyResult.Info> results) {
        return new PlayerHobbyResponse.Infos(
                results.stream()
                        .map(
                                result ->
                                        new PlayerHobbyResponse.Info(
                                                result.hobbyId(),
                                                result.name(),
                                                result.category(),
                                                result.customName(),
                                                result.detail(),
                                                result.proficiency(),
                                                result.status(),
                                                result.startedOn(),
                                                result.xp()
                                        )
                        )
                        .toList()
        );
    }

    public static PlayerHobbyCommand.Create toCreateCommand(Long hobbyId, PlayerHobbyRequest.Create request) {
        return new PlayerHobbyCommand.Create(
                hobbyId,
                request.customName(),
                request.detail(),
                request.proficiency(),
                request.status(),
                request.startedOn()
        );
    }

    public static PlayerHobbyResponse.Created toCreated(PlayerHobbyResult.Created result) {
        return new PlayerHobbyResponse.Created(
                result.hobbyId(),
                result.customName(),
                result.detail(),
                result.proficiency(),
                result.status(),
                result.startedOn(),
                result.xp()
        );
    }

    public static PlayerHobbyCommand.Change toChangeCommand(Long hobbyId, PlayerHobbyRequest.Update request) {
        return new PlayerHobbyCommand.Change(
                hobbyId,
                request.customName(),
                request.detail(),
                request.proficiency(),
                request.status(),
                request.startedOn()
        );
    }

    public static PlayerHobbyResponse.Changed toChanged(PlayerHobbyResult.Changed result) {
        return new PlayerHobbyResponse.Changed(
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
