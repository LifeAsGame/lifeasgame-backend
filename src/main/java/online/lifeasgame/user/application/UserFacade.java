package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentUserAccessor;
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
}
