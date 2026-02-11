package online.lifeasgame.character.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.PlayerResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface PlayerApiDraftSpecV1 {
    @Operation(summary = "캐릭터 시트 조회", description = "상태창 렌더링용(기본정보 + 대표칭호 + 장착정보)을 한 번에 내려줍니다.")
    ResponseEntity<ApiResponse<PlayerResponse.CharacterSheet>> characterSheet();
}
