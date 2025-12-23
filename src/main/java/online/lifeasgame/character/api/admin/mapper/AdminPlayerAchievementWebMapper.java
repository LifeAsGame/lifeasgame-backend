package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse;
import online.lifeasgame.character.application.result.PlayerAchievementResult;

public final class AdminPlayerAchievementWebMapper {

    private AdminPlayerAchievementWebMapper() {}

    public static AdminPlayerAchievementResponse.Granted toGranted(PlayerAchievementResult.Granted result) {
        return new AdminPlayerAchievementResponse.Granted(
                result.playerId(),
                result.achievementId(),
                result.code(),
                result.name(),
                result.category(),
                result.acquiredAt()
        );
    }
}
