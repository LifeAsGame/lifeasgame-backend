package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.HobbyResult;
import online.lifeasgame.character.api.player.response.HobbyResponse;

public class HobbyWebMapper {

    private HobbyWebMapper() {}

    public static HobbyResponse.Infos toHobbyInfos(List<HobbyResult.Info> infos) {
        return HobbyResponse.Infos.of(
                infos.stream()
                        .map(
                                hobbyInfo ->
                                        HobbyResponse.Info.of(
                                                hobbyInfo.hobbyId(),
                                                hobbyInfo.name(),
                                                hobbyInfo.category()
                                        )
                        )
                        .toList()
        );
    }
}
