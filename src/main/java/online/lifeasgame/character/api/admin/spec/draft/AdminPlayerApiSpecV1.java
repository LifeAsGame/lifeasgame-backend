package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminPlayerRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface AdminPlayerApiSpecV1 {

    @Operation(summary = "Player 단건 조회", description = "관리용 Player 상세를 조회합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.PlayerInfo>> get(
            @PathVariable Long playerId
    );

    @Operation(summary = "Player 검색", description = "userId/이름으로 Player를 검색합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.Players>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );


    @Operation(summary = "상태 이상 설정", description = "상태 이상을 덮어쓰기(set)로 설정합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.StatusEffectsSet>> setStatusEffects(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.SetStatusEffects request
    );

    @Operation(summary = "Player 이름 변경", description = "Player 이름을 변경합니다.")
    ResponseEntity<ApiResponse<AdminPlayerResponse.Renamed>> rename(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminPlayerRequest.Rename request
    );
}
