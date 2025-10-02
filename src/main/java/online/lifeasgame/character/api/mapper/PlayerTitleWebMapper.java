package online.lifeasgame.character.api.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.character.api.response.PlayerTitleResponse;

public class PlayerTitleWebMapper {

    private PlayerTitleWebMapper() {}

    public static PlayerTitleResponse.PlayerTitleInfos toPlayerTitleInfos(List<PlayerTitleResult.PlayerTitleInfo> playerTitleInfos) {
        return PlayerTitleResponse.PlayerTitleInfos.of(
                playerTitleInfos.stream()
                        .map(
                                playerTitleInfo ->
                                        PlayerTitleResponse.PlayerTitleInfo.of(
                                                playerTitleInfo.titleId(),
                                                playerTitleInfo.code(),
                                                playerTitleInfo.name(),
                                                playerTitleInfo.category(),
                                                playerTitleInfo.descMd(),
                                                playerTitleInfo.acquiredAt()
                                        )
                        )
                        .toList()
        );
    }
}

