package online.lifeasgame.user.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.user.api.admin.request.AdminUserRequest;
import online.lifeasgame.user.api.admin.response.AdminUserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface AdminUserApiSpecV1 {

    @Operation(summary = "유저 검색", description = "이메일/닉네임/상태로 유저 목록을 조회합니다.")
    ResponseEntity<ApiResponse<AdminUserResponse.UserList>> search(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "유저 상세 조회", description = "유저 상세 정보를 조회합니다.")
    ResponseEntity<ApiResponse<AdminUserResponse.UserInfo>> get(
            @PathVariable Long userId
    );

    @Operation(summary = "유저 상태 변경", description = "유저 상태를 변경합니다. (ACTIVE/BANNED/DELETED)")
    ResponseEntity<ApiResponse<AdminUserResponse.StatusChanged>> changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRequest.ChangeStatus request
    );

    @Operation(summary = "유저 닉네임 강제 변경", description = "운영 목적(규정 위반 등)으로 닉네임을 강제 변경합니다.")
    ResponseEntity<ApiResponse<AdminUserResponse.NicknameChanged>> forceChangeNickname(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRequest.ForceChangeNickname request
    );
}
