package online.lifeasgame.user.application.result;

import online.lifeasgame.user.domain.User;

public class UserResult {
    public record Created(Long id) {
        public static Created of(Long id) {
            return new Created(id);
        }
    }

    public record UserInfo(String email, String nickname) {
        public static UserInfo from(User user) {
            return new UserInfo(user.getEmail().getValue(), user.getNickname().getValue());
        }
    }
}
