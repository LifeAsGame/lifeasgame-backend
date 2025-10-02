package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import online.lifeasgame.character.api.player.response.AchievementResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface AchievementApiSpecV1 {

    @Operation(summary = "Achievement 목록 조회", description = "Achievement 목록 조회: category 설정 가능")
    ResponseEntity<ApiResponse<AchievementResponse.Infos>> achievementInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    );
}
