package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.response.TitleResponse;
import online.lifeasgame.character.application.result.TitleResult;

import java.util.List;

public final class TitleWebMapper {

    private TitleWebMapper() {}

    public static TitleResponse.Infos toTitleInfos(List<TitleResult.Info> results) {
        return new TitleResponse.Infos(
                results.stream()
                        .map(
                                result ->
                                        new TitleResponse.Info(
                                                result.titleId(),
                                                result.code(),
                                                result.name(),
                                                result.category(),
                                                result.descMd()
                                        )
                        )
                        .toList()
        );
    }

    public static TitleResponse.Info toTitleInfo(TitleResult.Info result) {
        return new TitleResponse.Info(
                result.titleId(),
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }
}
