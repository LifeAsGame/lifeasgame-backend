package online.lifeasgame.user.api.admin.response;

import java.time.Instant;

public final class AdminUserSettingResponse {

    private AdminUserSettingResponse() {
    }

    public record Settings(
            Long userId,
            int volume,
            String uiLayoutJson,
            String flagsJson,
            Instant updatedAt
    ) {}
}
