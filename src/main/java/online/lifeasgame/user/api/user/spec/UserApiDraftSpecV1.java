package online.lifeasgame.user.api.user.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.user.api.user.request.UserRequest;
import online.lifeasgame.user.api.user.response.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User API V2")
public interface UserApiDraftSpecV1 {

    @Operation(summary = "내 설정 수정", description = "현재 로그인한 유저의 설정(UserSetting)을 부분 수정합니다.")
    ResponseEntity<ApiResponse<UserResponse.Settings>> updateMySettings(
            @Valid @RequestBody UserRequest.UpdateSettings request
    );
}
