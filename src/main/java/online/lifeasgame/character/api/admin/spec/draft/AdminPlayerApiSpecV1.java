package online.lifeasgame.character.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.admin.request.AdminPlayerRequest;
import online.lifeasgame.character.api.admin.response.AdminPlayerResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface AdminPlayerApiSpecV1 {

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
