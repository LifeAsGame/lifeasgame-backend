package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.AchievementResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface AchievementApiSpecV1 {

    @Operation(summary = "Achievement 목록 조회", description = "업적 도감/목록용. category 필터 가능")
    ResponseEntity<ApiResponse<AchievementResponse.Infos>> achievementInfos(
            @RequestParam(name = "category", required = false) List<String> categories
    );
}
