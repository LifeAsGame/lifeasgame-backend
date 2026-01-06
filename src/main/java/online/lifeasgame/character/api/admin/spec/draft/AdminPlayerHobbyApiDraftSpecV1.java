package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.admin.response.AdminPlayerHobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminPlayerHobbyApiDraftSpecV1 {

    @Operation(summary = "Player Hobby 회수", description = "Player의 Hobby를 회수합니다.")
    ResponseEntity<ApiResponse<AdminPlayerHobbyResponse.Revoked>> revokeHobby(
            @PathVariable Long playerId,
            @PathVariable Long hobbyId
    );
}
