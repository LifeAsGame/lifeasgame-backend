package online.lifeasgame.character.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.character.api.player.request.PlayerHobbyRequest;
import online.lifeasgame.character.api.player.response.PlayerHobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Player Hobby API V1")
public interface PlayerHobbyApiSpecV1 {

    @Operation(summary = "Player 보유 Hobby 목록", description = "사용자가 보유한 취미 목록을 출력합니다.")
    ResponseEntity<ApiResponse<PlayerHobbyResponse.Infos>> playerHobbyInfos();

    @Operation(summary = "취미 추가", description = "사용자가 취미를 추가합니다.")
    ResponseEntity<ApiResponse<PlayerHobbyResponse.Created>> create(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.Create request
    );

    @Operation(summary = "취미 수정", description = "사용자가 취미를 수정합니다.")
    ResponseEntity<ApiResponse<PlayerHobbyResponse.Changed>> update(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.Update request
    );

    @Operation(summary = "취미 삭제", description = "사용자가 취미를 삭제합니다.")
    ResponseEntity<ApiResponse<Long>> delete(
            @PathVariable Long hobbyId
    );
}
