package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.admin.response.AdminPlayerTitleResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminPlayerTitleApiDraftSpecV1 {
    @Operation(summary = "Player Title 회수", description = "Player의 Title을 회수합니다.")
    ResponseEntity<ApiResponse<AdminPlayerTitleResponse.Revoked>> revokeTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    );
}
