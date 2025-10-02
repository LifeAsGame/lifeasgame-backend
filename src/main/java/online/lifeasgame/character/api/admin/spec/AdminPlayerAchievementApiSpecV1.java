package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse.GrantedAchievement;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminPlayerAchievementApiSpecV1 {

    @Operation(summary = "Player Achievement 지급", description = "Player에게 Achievement를 지급합니다")
    ResponseEntity<ApiResponse<GrantedAchievement>> grantAchievement(
            @PathVariable Long playerId,
            @PathVariable Long achievementId
    );
}
