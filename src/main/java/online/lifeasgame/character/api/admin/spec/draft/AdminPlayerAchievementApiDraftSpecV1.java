package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminPlayerAchievementApiDraftSpecV1 {

    @Operation(summary = "Player Achievement 회수", description = "Player의 Achievement를 회수합니다.")
    ResponseEntity<ApiResponse<AdminPlayerAchievementResponse.Revoked>> revokeAchievement(
            @PathVariable Long playerId,
            @PathVariable Long achievementId
    );
}
