package online.lifeasgame.user.api.user.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.user.api.user.request.UserSettingRequest;
import online.lifeasgame.user.api.user.response.UserSettingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserSettingApiSpecV1 {

    @Operation(summary = "내 설정 조회", description = "현재 로그인한 유저의 설정(UserSetting)을 조회합니다.")
    ResponseEntity<ApiResponse<UserSettingResponse.Settings>> getMySettings();

    @Operation(summary = "내 설정 수정", description = "현재 로그인한 유저의 설정(UserSetting)을 부분 수정합니다.")
    ResponseEntity<ApiResponse<UserSettingResponse.Settings>> updateMySettings(
            @Valid @RequestBody UserSettingRequest.UpdateSettings request
    );
}
