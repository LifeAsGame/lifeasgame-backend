package online.lifeasgame.user.presentation;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import online.lifeasgame.user.application.UserFacade;
import online.lifeasgame.user.application.UserService;
import online.lifeasgame.user.application.result.UserResult;
import online.lifeasgame.user.presentation.mapper.UserWebMapper;
import online.lifeasgame.user.presentation.request.UserRequest;
import online.lifeasgame.user.presentation.response.UserResponse;
import online.lifeasgame.user.presentation.spec.UserApiSpecV1;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserApiSpecV1 {

    private final UserService userService;
    private final UserFacade userFacade;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse.Created>> register(@Valid @RequestBody UserRequest.Register register) {
        UserResult.Created userResult = userService.register(UserWebMapper.toCommand(register));
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
