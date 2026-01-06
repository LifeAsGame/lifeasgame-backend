package online.lifeasgame.character.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.character.api.player.response.TitleResponse;
import online.lifeasgame.core.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface TitleApiDraftSpecV1 {

    @Operation(summary = "Title 단건 조회", description = "Title 상세(텍스트 UI 상세보기용)")
    ResponseEntity<ApiResponse<TitleResponse.Info>> titleInfo(
            @PathVariable Long titleId
    );
}
