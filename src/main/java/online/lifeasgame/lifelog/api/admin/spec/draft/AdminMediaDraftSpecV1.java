package online.lifeasgame.lifelog.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.admin.response.AdminMediaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminMediaDraftSpecV1 {

    @Operation(summary = "단건 조회(관리자, 플레이어 스코프)")
    ResponseEntity<ApiResponse<AdminMediaResponse.Info>> get(
            @PathVariable Long playerId,
            @PathVariable Long mediaId
    );

    @Operation(summary = "삭제(관리자)")
    ResponseEntity<ApiResponse<AdminMediaResponse.Deleted>> delete(
            @PathVariable Long playerId,
            @PathVariable Long mediaId
    );
}
