package online.lifeasgame.character.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminPlayerRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin API V1")
public interface AdminPlayerApiSpecV1 {

    @Operation(summary = "Player Exp 지급", description = "사용자에게 exp를 지급합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.ExpGranted>> grantExp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantExp request
    );

    @Operation(summary = "Player HP 상태 변경", description = "Player HP를 증가, 감소 시킵니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.CurrentHp>> setCurrentHp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeHp request
    );

    @Operation(summary = "Player 최대 HP 변경", description = "Player 초대 HP를 증가, 감소 시킵니다")
    ResponseEntity<ApiResponse<AdminPlayerResponse.HpCapacity>> setHpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeHpCapacity request
    );

    @Operation(summary = "Player MP 상태 변경", description = "Player MP를 증가, 감소 시킵니다")
    ResponseEntity<ApiResponse<AdminPlayerResponse.CurrentMp>> setCurrentMp(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeMp request
    );

    @Operation(summary = "Player 최대 MP 변경", description = "Player 최대 MP를 증가, 감소 시킵니다")
    ResponseEntity<ApiResponse<AdminPlayerResponse.MpCapacity>> setMpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.ChangeMpCapacity request
    );

    @Operation(summary = "코어 스탯 지급", description = "STR/AGI/DEX/INT/VIT/LUC 스탯을 지급(증감)합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.CoreStatsGranted>> grantCoreStats(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantCoreStats request
    );

    @Operation(summary = "상태 이상 설정", description = "상태 이상을 덮어쓰기(set)로 설정합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.StatusEffectsGranted>> grantStatusEffects(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.GrantStatusEffects request
    );

    @Operation(summary = "Player 이름 변경", description = "Player 이름을 변경합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.Renamed>> rename(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.Rename request
    );

    @Operation(summary = "Player 대표 Title 변경", description = "Player 대표 Title을 변경합니다")
    ResponseEntity<ApiResponse<AdminPlayerResponse.UpdatedTitle>> updateTitle(
            @PathVariable Long playerId,
            @PathVariable Long titleId
    );

    @Operation(summary = "Player 단건 조회", description = "관리용 Player 상세를 조회합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.PlayerInfo>> get(
            @PathVariable Long playerId
    );

    @Operation(summary = "Player 조회", description = "userId로 Player를 검색합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.Players>> getPlayersOfUser(
            @RequestParam(required = false) Long userId
    );
}
