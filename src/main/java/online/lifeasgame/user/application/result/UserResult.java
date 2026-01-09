package online.lifeasgame.user.application.result;

import online.lifeasgame.user.domain.User;
import online.lifeasgame.user.domain.UserSetting;

import java.time.Instant;

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

    public record NicknameChanged(Long userId, String nickname) {
    }

    public record PasswordChanged(Long userId) {
    }

    public record Deleted(Long userId, String status) {
    }

    public record Settings(
            Long userId,
            int volume,
            String uiLayoutJson,
            String flagsJson,
            Instant updatedAt
    ) {
        public static Settings from(UserSetting userSetting) {
            return new Settings(
                    userSetting.getUserId(),
                    userSetting.getVolume().getValue(),
                    userSetting.getUiLayoutJson(),
                    userSetting.getFlagsJson(),
                    userSetting.getUpdatedAt()
            );
        }
    }
}
