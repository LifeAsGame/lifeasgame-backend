package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.response.HobbyResponse;
import online.lifeasgame.character.application.result.HobbyResult;

import java.util.List;

public final class HobbyWebMapper {

    private HobbyWebMapper() {}

    public static HobbyResponse.Infos toInfos(List<HobbyResult.Info> results) {
        return new HobbyResponse.Infos(
                results.stream()
                        .map(
                                result ->
                                        new HobbyResponse.Info(
                                                result.hobbyId(),
                                                result.name(),
                                                result.category()
                                        )
                        )
                        .toList()
        );
    }

    public static HobbyResponse.Info toInfo(HobbyResult.Info result) {
        return new HobbyResponse.Info(
                result.hobbyId(),
                result.name(),
                result.category()
        );
    }
}
