package online.lifeasgame.user.api.user.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public final class UserSettingRequest {

    private UserSettingRequest() {
    }

    public record UpdateSettings(
            @Min(0) @Max(100) Integer volume,
            String uiLayoutJson,
            String flagsJson
    ) {
    }
}
