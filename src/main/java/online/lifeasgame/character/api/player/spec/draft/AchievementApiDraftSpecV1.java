package online.lifeasgame.character.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.AchievementResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AchievementApiDraftSpecV1 {

    @Operation(summary = "Achievement 단건 조회", description = "업적 상세(텍스트 UI 상세보기용)")
    ResponseEntity<ApiResponse<AchievementResponse.Info>> achievementInfo(
            @PathVariable Long achievementId
    );
}
