package online.lifeasgame.user.application.result;

import online.lifeasgame.user.domain.UserSetting;

import java.time.Instant;

public final class UserSettingResult {

    private UserSettingResult() {
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
