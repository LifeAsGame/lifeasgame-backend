package online.lifeasgame.lifelog.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.admin.response.AdminCollectionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface AdminCollectionDraftSpecV1 {

    @Operation(summary = "단건 조회(관리자, 플레이어 스코프)")
    ResponseEntity<ApiResponse<AdminCollectionResponse.Info>> get(
            @PathVariable Long playerId,
            @PathVariable Long collectionId
    );


    @Operation(summary = "컬렉션 삭제(관리자, 플레이어 스코프)")
    ResponseEntity<ApiResponse<AdminCollectionResponse.Deleted>> delete(
            @PathVariable Long playerId,
            @PathVariable Long collectionId
    );
}
