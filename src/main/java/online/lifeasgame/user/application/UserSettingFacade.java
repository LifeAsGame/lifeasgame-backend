package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.user.application.command.UserSettingCommand;
import online.lifeasgame.user.application.result.UserSettingResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSettingFacade {

    private final CurrentUserAccessor currentUser;
    private final UserSettingService userSettingService;

    public UserSettingResult.Settings getUserSettings() {
        return userSettingService.getSettings(currentUser.currentUserIdOrThrow());
    }

    public UserSettingResult.Settings updateSettings(UserSettingCommand.UpdateSettings command) {
        return userSettingService.updateSettings(currentUser.currentUserIdOrThrow(), command);
    }
}
