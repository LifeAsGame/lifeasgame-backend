package online.lifeasgame.user.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.user.api.admin.response.AdminUserResponse;
import org.springframework.http.ResponseEntity;
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
}
