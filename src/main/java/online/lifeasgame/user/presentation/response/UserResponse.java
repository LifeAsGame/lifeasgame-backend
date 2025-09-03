package online.lifeasgame.user.presentation.response;

public class UserResponse {
    public record Created(Long id) {
        public static Created of(Long id) {
            return new Created(id);
        }
    }

    public record UserInfo(String email, String name) {
        public static UserInfo of(String email, String name) {
            return new UserInfo(email, name);
        }
    }
}
