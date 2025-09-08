package online.lifeasgame.character.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.presentation.request.AdminPlayerRequest;
import online.lifeasgame.character.presentation.response.AdminPlayerResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin API V1")
public interface AdminPlayerApiSpecV1 {

    @Operation(summary = "Player Exp 지급", description = "사용자에게 exp를 지급합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.ExpGranted>> grantExp(@Valid @RequestBody AdminPlayerRequest.GrantExp grantExp);
}
