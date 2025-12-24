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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse.Created>> register(@Valid @RequestBody UserRequest.Register request) {
        UserResult.Created userResult = userService.register(UserWebMapper.toRegisterCommand(request));
        return ApiResponses.created(
                URI.create("/api/v1/users/" + userResult.id()),
                UserWebMapper.toCreated(userResult)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse.UserInfo>> me() {
        UserResult.UserInfo userInfo = userFacade.getUserInfo();
        return ApiResponses.ok(UserWebMapper.toUserInfo(userInfo));
    }
}
