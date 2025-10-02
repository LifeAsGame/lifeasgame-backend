package online.lifeasgame.character.api.player.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.api.player.response.PlayerAchievementResponse;

public class PlayerAchievementWebMapper {

    private PlayerAchievementWebMapper() {}

    public static PlayerAchievementResponse.Infos toPlayerAchievementInfos(List<PlayerAchievementResult.Info> infos) {
        return PlayerAchievementResponse.Infos.of(
                infos.stream()
                        .map(
                                playerAchievementInfo ->
                                        PlayerAchievementResponse.Info.of(
                                                playerAchievementInfo.achievementId(),
                                                playerAchievementInfo.code(),
                                                playerAchievementInfo.name(),
                                                playerAchievementInfo.category(),
                                                playerAchievementInfo.descMd(),
                                                playerAchievementInfo.acquiredAt()
                                        )
                        )
                        .toList()
        );
    }
}

