package online.lifeasgame.lifelog.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import online.lifeasgame.lifelog.api.admin.request.AdminCollectionRequest;
import online.lifeasgame.lifelog.api.admin.response.AdminCollectionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "LifeLog Collection API V1 (Admin)")
public interface AdminCollectionSpecV1 {

    @Operation(summary = "최근 조회(관리자, 플레이어 스코프)")
    ResponseEntity<List<AdminCollectionResponse.Info>> recent(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit
    );

    @Operation(summary = "검색(관리자, 플레이어 스코프)")
    ResponseEntity<List<AdminCollectionResponse.Info>> search(
            @PathVariable Long playerId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "titleLike") String titleLike,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "컬렉션 등록(관리자, 플레이어 스코프)")
    ResponseEntity<AdminCollectionResponse.Created> create(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminCollectionRequest.Create request
    );

    @Operation(summary = "컬렉션 수정(관리자, 플레이어 스코프)")
    ResponseEntity<AdminCollectionResponse.Info> update(
            @PathVariable Long playerId,
            @PathVariable Long collectionId,
            @Valid @RequestBody AdminCollectionRequest.Update request
    );
}
