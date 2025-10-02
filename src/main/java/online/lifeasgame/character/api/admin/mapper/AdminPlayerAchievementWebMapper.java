package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse;
import online.lifeasgame.character.application.result.PlayerAchievementResult;

public class AdminPlayerAchievementWebMapper {

    private AdminPlayerAchievementWebMapper() {}

    public static AdminPlayerAchievementResponse.Granted toGrantedAchievement(
            PlayerAchievementResult.Granted result
    ) {
        return AdminPlayerAchievementResponse.Granted.of(
                result.playerId(),
                result.achievementId(),
                result.code(),
                result.name(),
                result.category(),
                result.acquiredAt()
        );
    }
}
