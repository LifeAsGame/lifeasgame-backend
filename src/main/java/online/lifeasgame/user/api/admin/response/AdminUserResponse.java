package online.lifeasgame.user.api.admin.response;

import java.time.Instant;
import java.util.List;

public final class AdminUserResponse {

    private AdminUserResponse() {
    }

    public record UserList(
            List<UserSummary> users,
            PageInfo page
    ) {
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
    public record UserDetail(
            Long id,
            String email,
            String nickname,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {

    }
    public record StatusChanged(
            Long userId,
            String fromStatus,
            String toStatus,
            String reason,
            Instant changedAt
    ) {

    }
    public record NicknameChanged(
            Long userId,
            String oldNickname,
            String newNickname,
            String reason,
            Instant changedAt
    ) {

    }
    public record Settings(
            Long userId,
            int volume,
            String uiLayoutJson,
            String flagsJson,
            Long version,
            Instant updatedAt
    ) {

    }
}
