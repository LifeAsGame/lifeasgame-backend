package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminAchievementRequest;
import online.lifeasgame.character.api.admin.response.AdminAchievementResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminAchievementApiDraftSpecV1 {

    @Operation(summary = "Achievement 수정", description = "Achievement를 수정합니다.")
    ResponseEntity<ApiResponse<AdminAchievementResponse.Info>> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody AdminAchievementRequest.Update request
    );

    @Operation(summary = "Achievement 삭제", description = "Achievement를 삭제합니다.")
    ResponseEntity<ApiResponse<AdminAchievementResponse.Deleted>> delete(
            @PathVariable Long achievementId
    );
}
