package online.lifeasgame.character.presentation.mapper;

import java.util.List;
import online.lifeasgame.character.application.result.PlayerAchievementResult;
import online.lifeasgame.character.presentation.response.PlayerAchievementResponse;

public class PlayerAchievementWebMapper {

    private PlayerAchievementWebMapper() {}

    public static PlayerAchievementResponse.PlayerAchievementInfos toPlayerAchievementInfos(List<PlayerAchievementResult.PlayerAchievementInfo> playerAchievementInfos) {
        return PlayerAchievementResponse.PlayerAchievementInfos.of(
                playerAchievementInfos.stream()
                        .map(
                                playerAchievementInfo ->
                                        PlayerAchievementResponse.PlayerAchievementInfo.of(
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

