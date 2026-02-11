package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.response.AchievementResponse;
import online.lifeasgame.character.application.result.AchievementResult;

import java.util.List;

public final class AchievementWebMapper {

    private AchievementWebMapper() {}

    public static AchievementResponse.Infos toInfos(List<AchievementResult.Info> results) {
        return new AchievementResponse.Infos(
                results.stream()
                        .map(
                                result ->
                                        new AchievementResponse.Info(
                                                result.achievementId(),
                                                result.code(),
                                                result.name(),
                                                result.category(),
                                                result.descMd()
                                        )
                        )
                        .toList()
        );
    }

    public static AchievementResponse.Info toInfo(AchievementResult.Info result) {
        return new AchievementResponse.Info(
                result.achievementId(),
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }
}
