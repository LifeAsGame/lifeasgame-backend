package online.lifeasgame.user.application.query;

import online.lifeasgame.user.domain.UserStatus;

public interface UserQueryRepository {

    UserSearchQuery.SearchResult search(String email, String nickname, UserStatus status, int page, int size);
}
