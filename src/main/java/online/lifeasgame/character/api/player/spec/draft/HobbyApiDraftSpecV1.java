package online.lifeasgame.character.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.HobbyResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface HobbyApiDraftSpecV1 {

    @Operation(summary = "Hobby 단건 조회", description = "취미 상세(텍스트 UI 상세보기용)")
    ResponseEntity<ApiResponse<HobbyResponse.Info>> hobbyInfo(
            @PathVariable Long hobbyId
    );
}
