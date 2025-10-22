package online.lifeasgame.lifelog.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.lifeasgame.lifelog.api.admin.request.AdminCollectionRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminCollectionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "LifeLog Collection API V1 (Admin)")
public interface AdminCollectionSpecV1 {
    @Operation(summary = "컬렉션 등록(관리자, 플레이어 스코프)")
    ResponseEntity<AdminCollectionResponse.Created> create(Long playerId, AdminCollectionRequest.Create request);

    @Operation(summary = "컬렉션 수정(관리자, 플레이어 스코프)")
    ResponseEntity<AdminCollectionResponse.Info> update(
            Long playerId,
            Long collectionId,
            AdminCollectionRequest.Update request
    );

    @Operation(summary = "최근 조회(관리자, 플레이어 스코프)")
    ResponseEntity<List<AdminCollectionResponse.Info>> recent(Long playerId, Integer limit);

    @Operation(summary = "검색(관리자, 플레이어 스코프)")
    ResponseEntity<List<AdminCollectionResponse.Info>> search(
            @PathVariable Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );
}
