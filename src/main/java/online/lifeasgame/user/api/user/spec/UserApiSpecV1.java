package online.lifeasgame.user.api.user.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.user.api.user.request.UserRequest;
import online.lifeasgame.user.api.user.response.UserResponse;
import online.lifeasgame.user.api.user.response.UserResponse.Created;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User API V1")
public interface UserApiSpecV1 {

    @Operation(summary = "User 생성", description = "신규 유저를 등록합니다.")
    ResponseEntity<ApiResponse<Created>> register(@Valid @RequestBody UserRequest.Register registerRequest);

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 유저의 계정 정보(+텍스트 UI를 위한 상태 정보)를 조회합니다.")
    ResponseEntity<ApiResponse<UserResponse.UserInfo>> me();

    @Operation(summary = "이메일 사용 가능 여부", description = "회원가입 전 이메일 중복 여부를 확인합니다.")
    ResponseEntity<ApiResponse<UserResponse.Availability>> checkEmailAvailability(
            @RequestParam String email
    );

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
    ResponseEntity<ApiResponse<UserResponse.Deleted>> deleteMe(UserRequest.Delete request);
}
