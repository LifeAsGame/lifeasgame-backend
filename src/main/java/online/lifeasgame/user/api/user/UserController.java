package online.lifeasgame.user.api.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.user.api.user.mapper.UserWebMapper;
import online.lifeasgame.user.api.user.request.UserRequest;
import online.lifeasgame.user.api.user.response.UserResponse;
import online.lifeasgame.user.api.user.spec.UserApiSpecV1;
import online.lifeasgame.user.application.UserFacade;
import online.lifeasgame.user.application.UserService;
import online.lifeasgame.user.application.result.UserResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserApiSpecV1 {

    private final UserService userService;
    private final UserFacade userFacade;

    @Override
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse.Created>> register(
            @Valid @RequestBody UserRequest.Register request
    ) {
        UserResult.Created userResult = userService.register(UserWebMapper.toRegisterCommand(request));
        return ApiResponses.created(
                URI.create("/api/v1/users/" + userResult.id()),
                UserWebMapper.toCreated(userResult)
        );
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse.UserInfo>> me() {
        UserResult.UserInfo userInfo = userFacade.getUserInfo();
        return ApiResponses.ok(UserWebMapper.toUserInfo(userInfo));
    }

    @Override
    @GetMapping("/availability/email")
    public ResponseEntity<ApiResponse<UserResponse.Availability>> checkEmailAvailability(
            @RequestParam String email
    ) {
        UserResult.Availability availability = userService.checkEmailAvailability(email);
        return ApiResponses.ok(UserWebMapper.toAvailability(availability));
    }

    @Override
    @GetMapping("/availability/nickname")
    public ResponseEntity<ApiResponse<UserResponse.Availability>> checkNicknameAvailability(
            @RequestParam String nickname
    ) {
        UserResult.Availability availability = userService.checkNicknameAvailability(nickname);
        return ApiResponses.ok(UserWebMapper.toAvailability(availability));
    }

    @Override
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<UserResponse.NicknameChanged>> changeNickname(
            @Valid @RequestBody UserRequest.ChangeNickname request
    ) {
        UserResult.NicknameChanged nicknameChanged = userFacade.changeNickname(request.nickname());
        return ApiResponses.ok(UserWebMapper.toNicknameChanged(nicknameChanged));
    }

    @Override
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<UserResponse.PasswordChanged>> changePassword(
            @Valid @RequestBody UserRequest.ChangePassword request
    ) {
        UserResult.PasswordChanged passwordChanged = userFacade.changePassword(UserWebMapper.toChangePasswordCommand(request));
        return ApiResponses.ok(UserWebMapper.toPasswordChanged(passwordChanged));
    }

    @Override
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse.Deleted>> deleteMe(
            @Valid @RequestBody UserRequest.Delete request
    ) {
        UserResult.Deleted deleted = userFacade.delete(request.password());
        return ApiResponses.deleted(UserWebMapper.toDeleted(deleted));
    }

    @Override
    public ResponseEntity<ApiResponse<UserResponse.Settings>> getMySettings() {
        UserResult.Settings settings = userFacade.getUserSettings();
        return ApiResponses.ok(UserWebMapper.toSettings(settings));
    }
}
