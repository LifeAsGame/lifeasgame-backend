package online.lifeasgame.user.api.user.mapper;

import online.lifeasgame.user.api.user.request.UserSettingRequest;
import online.lifeasgame.user.api.user.response.UserSettingResponse;
import online.lifeasgame.user.application.command.UserSettingCommand;
import online.lifeasgame.user.application.result.UserSettingResult;

public final class UserSettingWebMapper {

    private UserSettingWebMapper() {}

    public static UserSettingResponse.Settings toSettings(UserSettingResult.Settings result) {
        return new UserSettingResponse.Settings(
                result.userId(),
                result.volume(),
                result.uiLayoutJson(),
                result.flagsJson(),
                result.updatedAt()
        );
    }

    public static UserSettingCommand.UpdateSettings toUpdateSettingsCommand(UserSettingRequest.UpdateSettings request) {
        return new UserSettingCommand.UpdateSettings(
                request.volume(),
                request.uiLayoutJson(),
                request.flagsJson()
        );
    }
}
