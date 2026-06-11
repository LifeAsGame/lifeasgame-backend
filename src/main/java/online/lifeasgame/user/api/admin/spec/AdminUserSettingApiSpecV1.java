package online.lifeasgame.user.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.user.api.admin.request.AdminUserSettingRequest;
import online.lifeasgame.user.api.admin.response.AdminUserSettingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminUserSettingApiSpecV1 {
    @Operation(summary = "유저 설정 수정", description = "특정 유저의 설정(UserSetting)을 부분 수정합니다.")
    ResponseEntity<ApiResponse<AdminUserSettingResponse.Settings>> updateSettings(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserSettingRequest.UpdateSettings request
    );
}
