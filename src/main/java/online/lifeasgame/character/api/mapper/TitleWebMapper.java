package online.lifeasgame.character.api.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.TitleResult;
import online.lifeasgame.character.api.response.TitleResponse;

public class TitleWebMapper {

    private TitleWebMapper() {}

    public static TitleResponse.TitleInfos toTitleInfos(List<TitleResult.TitleInfo> titleInfos) {
        return TitleResponse.TitleInfos.of(
                titleInfos.stream()
                        .map(
                                titleInfo ->
                                        TitleResponse.TitleInfo.of(
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
