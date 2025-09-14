package online.lifeasgame.character.presentation.mapper;

import online.lifeasgame.character.application.command.AdminAchievementCommand;
import online.lifeasgame.character.application.result.AdminAchievementResult;
import online.lifeasgame.character.presentation.request.AdminAchievementRequest;
import online.lifeasgame.character.presentation.response.AdminAchievementResponse;

public class AdminAchievementWebMapper {

    public static AdminAchievementCommand.CreateAchievement toCommand(AdminAchievementRequest.CreateAchievement request) {
        return AdminAchievementCommand.CreateAchievement.of(
                request.code(),
                request.name(),
                request.category(),
                request.descMd()
        );
    }

    public static AdminAchievementResponse.AchievementInfo toAchievementInfo(
            AdminAchievementResult.AchievementInfo result
    ) {
        return AdminAchievementResponse.AchievementInfo.of(
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }
}
