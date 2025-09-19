package online.lifeasgame.character.presentation.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.HobbyResult;
import online.lifeasgame.character.presentation.response.HobbyResponse;

public class HobbyWebMapper {

    private HobbyWebMapper() {}

    public static HobbyResponse.HobbyInfos toHobbyInfos(List<HobbyResult.HobbyInfo> hobbyInfos) {
        return HobbyResponse.HobbyInfos.of(
                hobbyInfos.stream()
                        .map(
                                hobbyInfo ->
                                        HobbyResponse.HobbyInfo.of(
                                                hobbyInfo.hobbyId(),
                                                hobbyInfo.name(),
                                                hobbyInfo.category()
                                        )
                        )
                        .toList()
        );
    }
}
