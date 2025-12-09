package online.lifeasgame.character.api.player.mapper;

import online.lifeasgame.character.api.player.response.PlayerAchievementResponse;
import online.lifeasgame.character.application.result.PlayerAchievementResult;

import java.util.List;

public final class PlayerAchievementWebMapper {

    private PlayerAchievementWebMapper() {}

    public static PlayerAchievementResponse.Infos toInfos(List<PlayerAchievementResult.Info> results) {
        return new PlayerAchievementResponse.Infos(
                results.stream()
                        .map(PlayerAchievementWebMapper::toInfo)
                        .toList()
        );
    }

    public static PlayerAchievementResponse.Info toInfo(PlayerAchievementResult.Info info) {
        return new PlayerAchievementResponse.Info(
                info.achievementId(),
                info.code(),
                info.name(),
                info.category(),
                info.descMd(),
                info.acquiredAt()
        );
    }
}

