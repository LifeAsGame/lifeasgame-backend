package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.response.PlayerTitleResponse;
import online.lifeasgame.character.application.result.PlayerTitleResult;

import java.util.List;

public final class PlayerTitleWebMapper {

    private PlayerTitleWebMapper() {}

    public static PlayerTitleResponse.Infos toPlayerTitleInfos(List<PlayerTitleResult.Info> results) {
        return new PlayerTitleResponse.Infos(
                results.stream()
                        .map(
                                result ->
                                        new PlayerTitleResponse.Info(
                                                result.titleId(),
                                                result.code(),
                                                result.name(),
                                                result.category(),
                                                result.descMd(),
                                                result.acquiredAt()
                                        )
                        )
                        .toList()
        );
    }
}

