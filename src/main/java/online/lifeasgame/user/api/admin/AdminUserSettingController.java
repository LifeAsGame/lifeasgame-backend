package online.lifeasgame.user.api.admin;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.user.api.admin.mapper.AdminUserSettingWebMapper;
import online.lifeasgame.user.api.admin.request.AdminUserSettingRequest;
import online.lifeasgame.user.api.admin.response.AdminUserSettingResponse;
import online.lifeasgame.user.api.admin.spec.AdminUserSettingApiSpecV1;
import online.lifeasgame.user.application.UserSettingService;
import online.lifeasgame.user.application.result.UserSettingResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminUserSettingController implements AdminUserSettingApiSpecV1 {

    private final UserSettingService userSettingService;


    @Override
    public ResponseEntity<ApiResponse<AdminUserSettingResponse.Settings>> updateSettings(
            Long userId,
            AdminUserSettingRequest.UpdateSettings request
    ) {
        UserSettingResult.Settings settings = userSettingService.updateSettings(
                userId,
                AdminUserSettingWebMapper.toUpdateSettingsCommand(request)
        );

        return ApiResponses.ok(AdminUserSettingWebMapper.toSettings(settings));
    }
}
