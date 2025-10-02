package online.lifeasgame.character.api.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.request.AdminPlayerRequest;
import online.lifeasgame.character.api.response.AdminPlayerResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin API V1")
public interface AdminPlayerApiSpecV1 {

    @Operation(summary = "Player Exp 지급", description = "사용자에게 exp를 지급합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.ExpGranted>> grantExp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantExp request
    );

    @Operation(summary = "Player HP 상태 변경", description = "Player HP를 증가, 감소 시킵니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.CurrentHp>> updateCurrentHp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeHp request
    );

    @Operation(summary = "Player 최대 HP 변경", description = "Player 초대 HP를 증가, 감소 시킵니다")
    ResponseEntity<ApiResponse<AdminPlayerResponse.HpCapacity>> updateHpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeHpCapacity request
    );

    @Operation(summary = "Player MP 상태 변경", description = "Player MP를 증가, 감소 시킵니다")
    ResponseEntity<ApiResponse<AdminPlayerResponse.CurrentMp>> updateCurrentMp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeMp request
    );

    @Operation(summary = "Player 최대 MP 변경", description = "Player 최대 MP를 증가, 감소 시킵니다")
    ResponseEntity<ApiResponse<AdminPlayerResponse.MpCapacity>> updateMpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeMpCapacity request
    );

    @Operation(summary = "Player core-stats 지급", description = "사용자의 core-stats를 증가, 감소 시킵니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.CoreStatsGranted>> grantCoreStats(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantCoreStats request
    );

    @Operation(summary = "Player Status Effects 설정", description = "사용자의 상태 이상을 처리합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.StatusEffectsGranted>> grantStatusEffects(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantStatusEffects request
    );

    @Operation(summary = "Player 대표 Title 변경", description = "Player 대표 Title을 변경합니다")
    ResponseEntity<ApiResponse<AdminPlayerResponse.UpdatedTitle>> updateTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    );
}
