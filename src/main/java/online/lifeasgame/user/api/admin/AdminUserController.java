package online.lifeasgame.user.api.admin;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.user.api.admin.mapper.AdminUserWebMapper;
import online.lifeasgame.user.api.admin.request.AdminUserRequest;
import online.lifeasgame.user.api.admin.response.AdminUserResponse;
import online.lifeasgame.user.api.admin.spec.AdminUserApiSpecV1;
import online.lifeasgame.user.application.UserService;
import online.lifeasgame.user.application.UserQueryService;
import online.lifeasgame.user.application.result.UserResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/users")
public class AdminUserController implements AdminUserApiSpecV1 {

    private final UserService userService;
    private final UserQueryService userQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<AdminUserResponse.UserList>> search(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UserResult.UserList userList = userQueryService.search(
                AdminUserWebMapper.toSearchQuery(email, nickname, status, page, size)
        );

        return ApiResponses.ok(AdminUserWebMapper.toUserList(userList));
    }

    @Override
    public ResponseEntity<ApiResponse<AdminUserResponse.UserInfo>> get(Long userId) {
        UserResult.UserInfo userInfo = userQueryService.getUserInfo(userId);
        return ApiResponses.ok(AdminUserWebMapper.toUserInfo(userInfo));
    }

    @Override
    public ResponseEntity<ApiResponse<AdminUserResponse.StatusChanged>> changeStatus(
            Long userId,
            AdminUserRequest.ChangeStatus request
    ) {
        UserResult.StatusChanged statusChanged = userService.changeStatus(userId, AdminUserWebMapper.toChangeStatusCommand(request));
        return ApiResponses.ok(AdminUserWebMapper.toStatusChanged(statusChanged));
    }

    @Override
    public ResponseEntity<ApiResponse<AdminUserResponse.NicknameChanged>> forceChangeNickname(
            Long userId,
            AdminUserRequest.ForceChangeNickname request
    ) {
        UserResult.NicknameChanged nicknameChanged = userService.changeNickname(userId, request.nickname());
        return ApiResponses.ok(AdminUserWebMapper.toNicknameChanged(nicknameChanged));
    }
}
