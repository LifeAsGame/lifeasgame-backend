package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminPlayerHobbyRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerHobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Player Hobby API V1")
public interface AdminPlayerHobbyApiSpecV1 {

    @Operation(summary = "Player Hobby 지급", description = "Player에게 Hobby를 지급합니다")
    ResponseEntity<ApiResponse<AdminPlayerHobbyResponse.Granted>> grantHobby(
            @PathVariable Long playerId,
            @PathVariable Long hobbyId,
            @Valid @RequestBody AdminPlayerHobbyRequest.Grant request
    );

    @Operation(summary = "Player Hobby 회수", description = "Player의 Hobby를 회수합니다.")
    ResponseEntity<ApiResponse<AdminPlayerHobbyResponse.Revoked>> revokeHobby(
            @PathVariable Long playerId,
            @PathVariable Long hobbyId
    );
}
