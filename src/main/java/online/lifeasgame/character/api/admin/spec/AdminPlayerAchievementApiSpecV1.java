package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import online.lifeasgame.character.api.admin.request.AdminPlayerHolderGrantRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse;
import online.lifeasgame.character.api.admin.response.AdminPlayerAchievementResponse.Granted;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AdminPlayerAchievementApiSpecV1 {

    @Operation(summary = "Player Achievement 조회", description = "Player의 Achievement holder summary를 조회합니다.")
    ResponseEntity<ApiResponse<AdminPlayerAchievementResponse.Infos>> getAchievements(
            @PathVariable @Positive Long playerId
    );

    @Operation(summary = "Player Achievement 지급", description = "Player에게 Achievement를 지급합니다")
    ResponseEntity<ApiResponse<Granted>> grantAchievement(
            @PathVariable @Positive Long playerId,
            @PathVariable @Positive Long achievementId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")
            String correlationId,
            @Valid @RequestBody AdminPlayerHolderGrantRequest.Grant request
    );

    @Operation(summary = "Player Achievement 회수", description = "Player의 Achievement를 회수합니다.")
    ResponseEntity<ApiResponse<AdminPlayerAchievementResponse.Revoked>> revokeAchievement(
            @PathVariable Long playerId,
            @PathVariable Long achievementId
    );
}
