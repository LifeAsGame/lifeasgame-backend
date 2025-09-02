package online.lifeasgame.user.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.user.presentation.request.UserRequest;
import online.lifeasgame.user.presentation.response.UserResponse.Created;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User API V1")
public interface UserApiSpecV1 {

    @Operation(summary = "User 생성", description = "신규 유저를 등록합니다.")
    ResponseEntity<ApiResponse<Created>> register(@Valid @RequestBody UserRequest.Register registerRequest);
}
