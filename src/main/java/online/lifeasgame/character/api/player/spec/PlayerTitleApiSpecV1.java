package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.PlayerTitleResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface PlayerTitleApiSpecV1 {

    @Operation(summary = "Player 보유 칭호 목록 출력", description = "사용자가 보유한 칭호 목록을 출력합니다")
    ResponseEntity<ApiResponse<PlayerTitleResponse.PlayerTitleInfos>> playerTitleInfos();
}
