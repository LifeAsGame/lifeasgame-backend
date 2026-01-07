package online.lifeasgame.user.application.result;

import online.lifeasgame.user.domain.User;

public final class UserResult {

    private UserResult() {
    }

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

    public record Availability(boolean isAvailable, String reason) {
    }
}
