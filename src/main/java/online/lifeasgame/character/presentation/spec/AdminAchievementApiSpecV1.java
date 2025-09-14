package online.lifeasgame.character.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.presentation.request.AdminAchievementRequest;
import online.lifeasgame.character.presentation.response.AdminAchievementResponse.AchievementInfo;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminAchievementApiSpecV1 {

    @Operation(summary = "Achievement 생성", description = "Achievement를 생성합니다")
    ResponseEntity<ApiResponse<AchievementInfo>> create(
            @Valid @RequestBody AdminAchievementRequest.CreateAchievement request
    );
}
