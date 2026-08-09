package online.lifeasgame.user.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.user.application.query.UserQuery;
import online.lifeasgame.user.application.query.UserSearchQuery;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.domain.Email;
import online.lifeasgame.user.domain.Nickname;
import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserStatus;
import online.lifeasgame.user.domain.error.UserError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserReader userReader;
    private final CurrentUserAccessor currentUserAccessor;

    public UserResult.UserInfo getUserInfo() {
        return getUserInfo(currentUserAccessor.currentUserIdOrThrow());
    }

    public UserResult.UserInfo getUserInfo(Long userId) {
        User user = userReader.findByIdOrElseThrow(userId);
        return UserResult.UserInfo.from(user);
    }

    public UserResult.Availability checkEmailAvailability(String email) {
        boolean available = !userReader.existsByEmail(Email.of(email));
        return new UserResult.Availability(available, UserError.EMAIL_DUPLICATE.message());
    }

    public UserResult.Availability checkNicknameAvailability(String nickname) {
        boolean available = !userReader.existsByNickname(Nickname.of(nickname));
        return new UserResult.Availability(available, UserError.NICKNAME_DUPLICATE.message());
    }

    public UserResult.UserList search(UserQuery.Search query) {
        int safePage = Math.max(query.page(), 0);
        int safeSize = Math.min(Math.max(query.size(), 1), 100);

        UserSearchQuery.SearchResult result = userReader.search(
                query.email(),
                query.nickname(),
                UserStatus.parseNullable(query.status()),
                safePage,
                safeSize
        );

        return UserResult.UserList.from(result.users(), safePage, safeSize, result.total());
    }
}
