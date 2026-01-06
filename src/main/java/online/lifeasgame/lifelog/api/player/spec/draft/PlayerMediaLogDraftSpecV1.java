package online.lifeasgame.lifelog.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.player.request.PlayerMediaLogRequest;
import online.lifeasgame.lifelog.api.player.response.PlayerMediaLogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PlayerMediaLogDraftSpecV1 {

    @Operation(summary = "미디어 로그 수정(플레이어)", description = "제목/카테고리/태그/진도/상태 등 편집용")
    ResponseEntity<ApiResponse<PlayerMediaLogResponse.Info>> update(
            @PathVariable Long mediaId,
            @Valid @RequestBody PlayerMediaLogRequest.Update request
    );

    @Operation(summary = "삭제(플레이어)")
    ResponseEntity<ApiResponse<PlayerMediaLogResponse.Deleted>> delete(
            @PathVariable Long mediaId
    );
}
