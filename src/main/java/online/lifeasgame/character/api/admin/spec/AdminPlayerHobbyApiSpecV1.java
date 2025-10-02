package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminPlayerHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerHobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminPlayerHobbyApiSpecV1 {

    @Operation(summary = "Player Hobby 지급", description = "Player에게 Hobby를 지급합니다")
    ResponseEntity<ApiResponse<AdminPlayerHobbyResponse.Granted>> grantHobby(
            @PathVariable Long playerId,
            @PathVariable Long hobbyId,
            @Valid @RequestBody AdminPlayerHobbyRequest.Grant request
    );
}
