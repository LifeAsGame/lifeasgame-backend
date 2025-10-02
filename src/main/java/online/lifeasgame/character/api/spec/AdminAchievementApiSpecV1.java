package online.lifeasgame.character.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.request.AdminAchievementRequest;
import online.lifeasgame.character.api.response.AdminAchievementResponse.AchievementInfo;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminAchievementApiSpecV1 {

    @Operation(summary = "Achievement 생성", description = "Achievement를 생성합니다")
    @Schema(allowableValues = {"STORY", "COMBAT", "EXPLORATION", "COLLECTION", "SOCIAL", "ECONOMY", "SKILL", "DAILY"})
    ResponseEntity<ApiResponse<AchievementInfo>> create(
            @Valid @RequestBody AdminAchievementRequest.CreateAchievement request
    );
}
