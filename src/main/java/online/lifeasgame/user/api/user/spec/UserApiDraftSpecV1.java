package online.lifeasgame.user.api.user.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.user.api.user.request.UserRequest;
import online.lifeasgame.user.api.user.response.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User API V2")
public interface UserApiDraftSpecV1 {

    @Operation(summary = "닉네임 사용 가능 여부", description = "닉네임 중복 여부를 확인합니다.")
    ResponseEntity<ApiResponse<UserResponse.Availability>> checkNicknameAvailability(
            @RequestParam String nickname
    );

    @Operation(summary = "닉네임 변경", description = "현재 로그인한 유저의 닉네임을 변경합니다.")
    ResponseEntity<ApiResponse<UserResponse.NicknameChanged>> changeNickname(
            @Valid @RequestBody UserRequest.ChangeNickname request
    );

    @Operation(summary = "비밀번호 변경", description = "현재 로그인한 유저의 비밀번호를 변경합니다.")
    ResponseEntity<ApiResponse<UserResponse.PasswordChanged>> changePassword(
            @Valid @RequestBody UserRequest.ChangePassword request
    );

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 유저를 탈퇴 처리합니다. (권장: soft delete)")
    ResponseEntity<ApiResponse<UserResponse.Deleted>> deleteMe();

    @Operation(summary = "내 설정 조회", description = "현재 로그인한 유저의 설정(UserSetting)을 조회합니다.")
    ResponseEntity<ApiResponse<UserResponse.Settings>> getMySettings();

    @Operation(summary = "내 설정 수정", description = "현재 로그인한 유저의 설정(UserSetting)을 부분 수정합니다.")
    ResponseEntity<ApiResponse<UserResponse.Settings>> updateMySettings(
            @Valid @RequestBody UserRequest.UpdateSettings request
    );
}
