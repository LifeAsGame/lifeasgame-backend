package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.user.application.command.UserCommand;
import online.lifeasgame.user.application.result.UserResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final CurrentUserAccessor currentUser;
    private final UserService userService;

    public UserResult.UserInfo getUserInfo() {
        return userService.getUserInfo(currentUser.currentUserIdOrThrow());
    }

    public UserResult.NicknameChanged changeNickname(String nickname) {
        return userService.changeNickname(currentUser.currentUserIdOrThrow(), nickname);
    }

    public UserResult.PasswordChanged changePassword(UserCommand.ChangePassword command) {
        return userService.changePassword(currentUser.currentUserIdOrThrow(), command);
    }

    public UserResult.Deleted delete(String password) {
        return userService.delete(currentUser.currentUserIdOrThrow(), password);
    }
}
