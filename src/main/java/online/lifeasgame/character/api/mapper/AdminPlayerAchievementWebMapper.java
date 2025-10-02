package online.lifeasgame.character.api.mapper;

import online.lifeasgame.character.application.result.AdminPlayerAchievementResult;
import online.lifeasgame.character.api.response.AdminPlayerAchievementResponse;

public class AdminPlayerAchievementWebMapper {

    private AdminPlayerAchievementWebMapper() {}

    public static AdminPlayerAchievementResponse.GrantedAchievement toGrantedAchievement(
            AdminPlayerAchievementResult.GrantedAchievement result
    ) {
        return AdminPlayerAchievementResponse.GrantedAchievement.of(
                result.playerId(),
                result.achievementId(),
                result.code(),
                result.name(),
                result.category(),
                result.acquiredAt()
        );
    }
}
