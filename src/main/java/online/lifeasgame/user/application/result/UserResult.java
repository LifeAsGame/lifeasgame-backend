package online.lifeasgame.user.application.result;

public class UserResult {
    public record Created(Long id) {
        public static Created of(Long id) {
            return new Created(id);
        }
    }
}
