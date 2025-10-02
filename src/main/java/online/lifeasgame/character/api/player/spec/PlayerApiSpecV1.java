package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.player.request.PlayerRequest;
import online.lifeasgame.character.api.player.response.PlayerResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Player API V1")
public interface PlayerApiSpecV1 {

    @Operation(summary = "Player 생성", description = "신규 Player를 생성합니다")
    ResponseEntity<ApiResponse<PlayerResponse.Created>> linkStart(@Valid @RequestBody PlayerRequest.Register request);

    @Operation(summary = "Player 정보 조회", description = "Player 정보를 조회합니다.")
    ResponseEntity<ApiResponse<PlayerResponse.Info>> playerInfo();

    @Operation(summary = "Player 대표 Title 변경", description = "Player 대표 Title을 변경합니다")
    ResponseEntity<ApiResponse<PlayerResponse.Updated>> updateTitle(
            @PathVariable Long titleId
    );
}
