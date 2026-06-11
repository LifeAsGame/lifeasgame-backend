package online.lifeasgame.user.application.result;

import online.lifeasgame.user.application.query.UserSearchQuery;
import online.lifeasgame.user.domain.User;

import java.time.Instant;
import java.util.List;

public final class UserResult {

    private UserResult() {
    }

    public record AuthCredential(Long userId) {}

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

    public record NicknameChanged(
            Long userId,
            String oldNickname,
            String newNickname,
            Instant changedAt
    ) {
    }

    public record PasswordChanged(Long userId) {
    }

    public record Deleted(Long userId, String status) {
    }

    public record UserList(
            List<UserSummary> users,
            PageInfo page
    ) {
        public static UserList from(List<UserSearchQuery.UserRow> users, int page, int size, long total) {
            return new UserList(
                    users.stream()
                            .map(
                                    user -> new UserSummary(
                                            user.id(),
                                            user.email(),
                                            user.nickname(),
                                            user.status(),
                                            user.createdAt()
                                    )
                            )
                            .toList(),
                    new PageInfo(page, size, total)
            );
        }

        public record UserSummary(
                Long id,
                String email,
                String nickname,
                String status,
                Instant createdAt
        ) {

        }
        public record PageInfo(
                int page,
                int size,
                long totalElements
        ) {
        }
    }

    public record StatusChanged(
            Long userId,
            String fromStatus,
            String toStatus,
            String reason,
            Instant changedAt
    ) {
    }
}
