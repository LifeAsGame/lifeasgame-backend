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

    @Operation(summary = "내 Player 조회", description = "상태창에 필요한 Player 기본 정보를 조회합니다.")
    ResponseEntity<ApiResponse<PlayerResponse.Info>> me();

    @Operation(summary = "Player 생성(링크 시작)", description = "User 계정 기준으로 Player를 생성합니다.")
    ResponseEntity<ApiResponse<PlayerResponse.Created>> register(
            @Valid @RequestBody PlayerRequest.Register request
    );

    @Operation(summary = "대표 Title 변경", description = "Player의 대표 Title을 변경합니다.")
    ResponseEntity<ApiResponse<PlayerResponse.UpdatedTitle>> updateTitle(
            @PathVariable Long titleId
    );
}
