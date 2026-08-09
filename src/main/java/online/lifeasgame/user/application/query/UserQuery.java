package online.lifeasgame.user.application.query;

public final class UserQuery {

    private UserQuery() {
    }

    public record Search(
            String email,
            String nickname,
            String status,
            int page,
            int size
    ) {
    }
}
