// ADMIN CONTROLLER SPEC
package online.lifeasgame.lifelog.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.lifelog.api.admin.request.AdminMediaRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminMediaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "LifeLog Media API V1 (Admin)")
public interface AdminMediaSpecV1 {

    @Operation(summary = "최근 조회(관리자, 플레이어 스코프)")
    ResponseEntity<List<AdminMediaResponse.Info>> recent(Long playerId, Integer limit);

    @Operation(summary = "검색(관리자, 플레이어 스코프)")
    ResponseEntity<List<AdminMediaResponse.Info>> search(
            Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "미디어 생성(관리자, 플레이어 스코프)")
    ResponseEntity<AdminMediaResponse.Created> create(Long playerId, AdminMediaRequest.Create request);

    @Operation(summary = "평점 반영(관리자)")
    ResponseEntity<AdminMediaResponse.Info> rate(Long playerId, Long mediaId, AdminMediaRequest.Rate request);

    @Operation(summary = "에피소드 진행(관리자)")
    ResponseEntity<AdminMediaResponse.Info> advance(Long playerId, Long mediaId, AdminMediaRequest.Advance request);

    @Operation(summary = "상태 변경(관리자)")
    ResponseEntity<AdminMediaResponse.Info> markStatus(Long playerId, Long mediaId, AdminMediaRequest.MarkStatus request);

    @Operation(summary = "리와치(관리자)")
    ResponseEntity<AdminMediaResponse.Info> rewatch(Long playerId, Long mediaId);

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
