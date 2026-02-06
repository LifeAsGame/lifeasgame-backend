package online.lifeasgame.user.api.admin.mapper;

import online.lifeasgame.user.api.admin.request.AdminUserSettingRequest;
import online.lifeasgame.user.api.admin.response.AdminUserSettingResponse;
import online.lifeasgame.user.application.command.UserSettingCommand;
import online.lifeasgame.user.application.result.UserSettingResult;

public final class AdminUserSettingWebMapper {

    private AdminUserSettingWebMapper() {}

    public static AdminUserSettingResponse.Settings toSettings(UserSettingResult.Settings result) {
        return new AdminUserSettingResponse.Settings(
                result.userId(),
                result.volume(),
                result.uiLayoutJson(),
                result.flagsJson(),
                result.updatedAt()
        );
    }

    public static UserSettingCommand.UpdateSettings toUpdateSettingsCommand(AdminUserSettingRequest.UpdateSettings request) {
        return new UserSettingCommand.UpdateSettings(
                request.volume(),
                request.uiLayoutJson(),
                request.flagsJson()
        );
    }
}
