package online.lifeasgame.inventory.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.admin.response.AdminItemResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface AdminItemApiDraftSpecV1 {

    @Operation(summary = "아이템 상세(관리자)", description = "아이템 상세를 조회합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Detail>> get(@PathVariable Long itemId);

    @Operation(summary = "아이템 목록(관리자)", description = "아이템 목록을 검색합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Page<AdminItemResponse.Summary>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String rarity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );
}
