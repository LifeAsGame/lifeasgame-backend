package online.lifeasgame.user.api.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.user.api.user.mapper.UserSettingWebMapper;
import online.lifeasgame.user.api.user.request.UserSettingRequest;
import online.lifeasgame.user.api.user.response.UserSettingResponse;
import online.lifeasgame.user.api.user.spec.UserSettingApiSpecV1;
import online.lifeasgame.user.application.UserSettingService;
import online.lifeasgame.user.application.result.UserSettingResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserSettingController implements UserSettingApiSpecV1 {

    private final UserSettingService userSettingService;

    @Override
    @GetMapping("/me/settings")
    public ResponseEntity<ApiResponse<UserSettingResponse.Settings>> getMySettings() {
        UserSettingResult.Settings settings = userSettingService.getSettings();
        return ApiResponses.ok(UserSettingWebMapper.toSettings(settings));
    }

    @Override
    @PatchMapping("/me/settings")
    public ResponseEntity<ApiResponse<UserSettingResponse.Settings>> updateMySettings(
            @Valid @RequestBody UserSettingRequest.UpdateSettings request
    ) {
        UserSettingResult.Settings settings = userSettingService.updateSettings(UserSettingWebMapper.toUpdateSettingsCommand(request));
        return ApiResponses.ok(UserSettingWebMapper.toSettings(settings));
    }
}
