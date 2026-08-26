package online.lifeasgame.inventory.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.admin.request.AdminItemRequest;
import online.lifeasgame.inventory.api.admin.response.AdminItemResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Item API V1 (Admin)")
public interface AdminItemApiSpecV1 {

    @Operation(summary = "아이템 목록(관리자)", description = "아이템 정의를 bounded search로 조회합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Page<AdminItemResponse.Summary>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String rarity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "아이템 상세(관리자)", description = "아이템 정의 상세를 조회합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Detail>> getItem(
            @PathVariable @Positive Long itemId
    );

    @Operation(summary = "아이템 등록", description = "아이템을 신규 등록합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Id>> create(@Valid @RequestBody AdminItemRequest.Create request);

    @Operation(summary = "아이템 수정", description = "아이템 정의를 요청 값 기준으로 수정합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Id>> update(
            @PathVariable Long itemId,
            @Valid @RequestBody AdminItemRequest.Update request
    );

    @Operation(summary = "아이템 삭제", description = "아이템을 삭제합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Deleted>> delete(@PathVariable Long itemId);
}
