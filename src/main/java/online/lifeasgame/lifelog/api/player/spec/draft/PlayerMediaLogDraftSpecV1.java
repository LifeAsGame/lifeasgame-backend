package online.lifeasgame.lifelog.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.response.PlayerMediaLogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface PlayerMediaLogDraftSpecV1 {

    @Operation(summary = "삭제(플레이어)")
    ResponseEntity<ApiResponse<PlayerMediaLogResponse.Deleted>> delete(
            @PathVariable Long mediaId
    );
}
