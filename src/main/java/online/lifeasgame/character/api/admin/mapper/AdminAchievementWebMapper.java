package online.lifeasgame.character.api.admin.mapper;

import online.lifeasgame.character.api.admin.request.AdminAchievementRequest;
import online.lifeasgame.character.api.admin.response.AdminAchievementResponse;
import online.lifeasgame.character.application.command.AchievementCommand;
import online.lifeasgame.character.application.result.AchievementResult;

public class AdminAchievementWebMapper {

    public static AchievementCommand.Create toCommand(AdminAchievementRequest.Create request) {
        return AchievementCommand.Create.of(
                request.code(),
                request.name(),
                request.category(),
                request.descMd()
        );
    }

    public static AdminAchievementResponse.Info toAchievementInfo(
            AchievementResult.Info result
    ) {
        return AdminAchievementResponse.Info.of(
                result.code(),
                result.name(),
                result.category(),
                result.descMd()
        );
    }
}
