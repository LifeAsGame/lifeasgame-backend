package online.lifeasgame.character.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.presentation.request.PlayerRequest;
import online.lifeasgame.character.presentation.response.PlayerResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Player API V1")
public interface PlayerApiSpecV1 {

    @Operation(summary = "Player 생성", description = "신규 Player를 생성합니다")
    ResponseEntity<ApiResponse<PlayerResponse.Created>> linkStart(@Valid @RequestBody PlayerRequest.Register request);

    @Operation(summary = "Player 정보 조회", description = "Player 정보를 조회합니다.")
    ResponseEntity<ApiResponse<PlayerResponse.PlayerInfo>> playerInfo();

    @Operation(summary = "Player HP 상태 변경", description = "Player HP를 증가, 감소 시킵니다.")
    ResponseEntity<ApiResponse<PlayerResponse.CurrentHp>> updateCurrentHp(
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerRequest.ChangeHp request
    );

    @Operation(summary = "Player 최대 HP 변경", description = "Player 초대 HP를 증가, 감소 시킵니다")
    ResponseEntity<ApiResponse<PlayerResponse.HpCapacity>> updateHpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerRequest.ChangeHpCapacity request
    );

    @Operation(summary = "Player MP 상태 변경", description = "Player MP를 증가, 감소 시킵니다")
    ResponseEntity<ApiResponse<PlayerResponse.CurrentMp>> updateCurrentMp(
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerRequest.ChangeMp request
    );

    @Operation(summary = "Player 최대 MP 변경", description = "Player 최대 MP를 증가, 감소 시킵니다")
    ResponseEntity<ApiResponse<PlayerResponse.MpCapacity>> updateMpCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody PlayerRequest.ChangeMpCapacity request
    );
}
