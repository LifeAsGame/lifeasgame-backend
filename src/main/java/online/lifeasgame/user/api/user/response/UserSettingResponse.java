package online.lifeasgame.user.api.user.response;

import java.time.Instant;

public final class UserSettingResponse {

    private UserSettingResponse() {
    }

    public record Settings(
            Long userId,
            int volume,
            String uiLayoutJson,
            String flagsJson,
            Instant updatedAt
    ) {}
}
