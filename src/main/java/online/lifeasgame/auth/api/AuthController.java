package online.lifeasgame.auth.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.auth.api.request.AuthRequest;
import online.lifeasgame.auth.application.AuthFacade;
import online.lifeasgame.auth.application.result.AuthResult;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthFacade authFacade;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResult.TokenPair>> login(
            @Valid @RequestBody AuthRequest.Login req
    ) {
        return ApiResponses.ok(authFacade.login(req.email(), req.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResult.TokenPair>> refresh(
            @Valid @RequestBody AuthRequest.Refresh req
    ) {
        return ApiResponses.ok(authFacade.refresh(req.refreshToken()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResult.RegisterResult>> register(
            @Valid @RequestBody AuthRequest.Register req
    ) {
        return ApiResponses.ok(authFacade.register(req.email(), req.password(), req.nickname()));
    }
}
