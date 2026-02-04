package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse;
import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse.Granted;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminPlayerAchievementApiSpecV1 {

    @Operation(summary = "Player Achievement 지급", description = "Player에게 Achievement를 지급합니다")
    ResponseEntity<ApiResponse<Granted>> grantAchievement(
            @PathVariable Long playerId,
            @PathVariable Long achievementId
    );

    @Operation(summary = "Player Achievement 회수", description = "Player의 Achievement를 회수합니다.")
    ResponseEntity<ApiResponse<AdminPlayerAchievementResponse.Revoked>> revokeAchievement(
            @PathVariable Long playerId,
            @PathVariable Long achievementId
    );
}
