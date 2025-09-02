package online.lifeasgame.user.presentation.response;

public class UserResponse {
    public record Created(Long id) {
        public static Created of(Long id) {
            return new Created(id);
        }
    }
}
