package online.lifeasgame.user.application.query;

import java.time.Instant;
import java.util.List;

public final class UserSearchQuery {

    private UserSearchQuery() {}

    public record SearchResult(List<UserRow> users, long total) {}

    public record UserRow(Long id, String email, String nickname, String status, Instant createdAt) {}
}
