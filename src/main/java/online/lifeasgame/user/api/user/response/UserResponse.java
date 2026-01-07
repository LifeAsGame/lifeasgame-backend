package online.lifeasgame.user.api.user.response;

import java.time.Instant;
import java.util.List;

public final class UserResponse {

    private UserResponse() {
    }

    public record Created(Long id) {
    }

    public record UserInfo(
            UserSummary user,
            PlayerHint player,
            UiHint ui
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

        public record UiHint(List<String> nextActions, Badges badges) {
            public record Badges(int notifications, int pendingRewards) {
            }
        }
    }

    public record Availability(boolean available, String reason) {}

    public record NicknameChanged(Long userId, String nickname) {}

    public record PasswordChanged(Long userId) {}

    public record Deleted(Long userId, String status) {}

    public record Settings(
            Long userId,
            int volume,
            String uiLayoutJson,
            String flagsJson,
            Long version,
            Instant updatedAt
    ) {}
}
