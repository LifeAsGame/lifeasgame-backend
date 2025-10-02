package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.AchievementResult;
import online.lifeasgame.character.api.player.response.AchievementResponse;

public class AchievementWebMapper {

    private AchievementWebMapper() {}

    public static AchievementResponse.Infos toAchievementInfos(List<AchievementResult.Info> infos) {
        return AchievementResponse.Infos.of(
                infos.stream()
                        .map(
                                achievementInfo ->
                                        AchievementResponse.Info.of(
                                                achievementInfo.code(),
                                                achievementInfo.name(),
                                                achievementInfo.category(),
                                                achievementInfo.descMd()
                                        )
                        )
                        .toList()
        );
    }
}
