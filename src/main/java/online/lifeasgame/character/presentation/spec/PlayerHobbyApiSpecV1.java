package online.lifeasgame.character.presentation.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.character.presentation.request.PlayerHobbyRequest;
import online.lifeasgame.character.presentation.response.PlayerHobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PlayerHobbyApiSpecV1 {

    @Operation(summary = "Player 보유 자격증 목록 출력", description = "사용자가 보유한 자격증 목록을 출력합니다")
    ResponseEntity<ApiResponse<PlayerHobbyResponse.PlayerHobbyInfos>> playerHobbyInfos();

    @Operation(summary = "Player Hobby 생성", description = "Player에게 Hobby를 생성합니다")
    ResponseEntity<ApiResponse<PlayerHobbyResponse.CreatedPlayerHobby>> createPlayerHobby(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.CreatePlayerHobby request
    );

    @Operation(summary = "Player 자격증 변경", description = "사용자의 자격증 정보를 변경합니다")
    ResponseEntity<ApiResponse<PlayerHobbyResponse.ChangedPlayerHobby>> updatePlayerHobby(
            @PathVariable Long hobbyId,
            @Valid @RequestBody PlayerHobbyRequest.ChangePlayerHobby request
    );

    @Operation(summary = "Player 자격증 삭제", description = "사용자의 자격증을 제거합니다")
    ResponseEntity<ApiResponse<Long>> deletePlayerHobby(@PathVariable Long hobbyId);
}
