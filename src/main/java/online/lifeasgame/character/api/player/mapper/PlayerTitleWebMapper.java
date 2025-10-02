package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.PlayerTitleResult;
import online.lifeasgame.character.api.player.response.PlayerTitleResponse;

public class PlayerTitleWebMapper {

    private PlayerTitleWebMapper() {}

    public static PlayerTitleResponse.Infos toPlayerTitleInfos(List<PlayerTitleResult.Info> infos) {
        return PlayerTitleResponse.Infos.of(
                infos.stream()
                        .map(
                                playerTitleInfo ->
                                        PlayerTitleResponse.Info.of(
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

