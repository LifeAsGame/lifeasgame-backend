package online.lifeasgame.user.api.admin.response;

import online.lifeasgame.user.api.user.response.UserResponse;

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

    public record UserInfo(
            UserResponse.UserInfo.UserSummary user,
            UserResponse.UserInfo.PlayerHint player,
            UserResponse.UserInfo.UiHint ui
    ) {
        public record UserSummary(
                Long id,
                String email,
                String nickname,
                String status
        ) {
        }

        public record PlayerHint(boolean exists, Long playerId) {
        }

        public record UiHint(List<String> nextActions, UserResponse.UserInfo.UiHint.Badges badges) {
            public record Badges(int notifications, int pendingRewards) {
            }
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

    public record NicknameChanged(
            Long userId,
            String oldNickname,
            String newNickname,
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
