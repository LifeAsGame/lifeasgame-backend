package online.lifeasgame.user.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.user.api.admin.request.AdminUserRequest;
import online.lifeasgame.user.api.admin.response.AdminUserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin User API V1")
public interface AdminUserApiDraftSpecV1 {

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
