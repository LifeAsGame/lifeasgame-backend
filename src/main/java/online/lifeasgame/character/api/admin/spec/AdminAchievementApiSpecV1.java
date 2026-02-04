package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminAchievementRequest;
import online.lifeasgame.character.api.admin.response.AdminAchievementResponse;
import online.lifeasgame.character.api.admin.response.AdminAchievementResponse.Info;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Admin Achievement API V1")
public interface AdminAchievementApiSpecV1 {

    @Operation(summary = "Achievement 생성", description = "Achievement를 생성합니다")
    @Schema(allowableValues = {"STORY", "COMBAT", "EXPLORATION", "COLLECTION", "SOCIAL", "ECONOMY", "SKILL", "DAILY"})
    ResponseEntity<ApiResponse<Info>> create(
            @Valid @RequestBody AdminAchievementRequest.Create request
    );

    @Operation(summary = "Achievement 목록 조회", description = "Achievement 목록을 조회합니다. category 필터 가능")
    ResponseEntity<ApiResponse<AdminAchievementResponse.Infos>> list(
            @RequestParam(name = "category", required = false) List<String> categories
    );
}
