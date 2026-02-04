package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.admin.response.AdminAchievementResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminAchievementApiDraftSpecV1 {

    @Operation(summary = "Achievement 삭제", description = "Achievement를 삭제합니다.")
    ResponseEntity<ApiResponse<AdminAchievementResponse.Deleted>> delete(
            @PathVariable Long achievementId
    );
}
