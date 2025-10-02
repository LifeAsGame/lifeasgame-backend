package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.TitleResult;
import online.lifeasgame.character.api.player.response.TitleResponse;

public class TitleWebMapper {

    private TitleWebMapper() {}

    public static TitleResponse.Infos toTitleInfos(List<TitleResult.Info> infos) {
        return TitleResponse.Infos.of(
                infos.stream()
                        .map(
                                titleInfo ->
                                        TitleResponse.Info.of(
                                                titleInfo.code(),
                                                titleInfo.name(),
                                                titleInfo.category(),
                                                titleInfo.descMd()
                                        )
                        )
                        .toList()
        );
    }
}
