package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminAchievementRequest;
import online.lifeasgame.character.api.admin.response.AdminAchievementResponse;
import online.lifeasgame.character.application.command.AchievementCommand;
import online.lifeasgame.character.application.result.AchievementResult;

public final class AdminAchievementWebMapper {

    private AdminAchievementWebMapper() {}

    public static AchievementCommand.Create toCreateCommand(AdminAchievementRequest.Create request) {
        return new AchievementCommand.Create(
                request.code(),
                request.name(),
                request.category(),
                request.descMd()
        );
    }

    public static AdminAchievementResponse.Info toInfo(AchievementResult.Info result) {
        return new AdminAchievementResponse.Info(
                result.achievementId(),
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }
}
