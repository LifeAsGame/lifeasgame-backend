package online.lifeasgame.inventory.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.admin.request.AdminItemRequest;
import online.lifeasgame.inventory.api.admin.reseponse.AdminItemResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Item API V1 (Admin)")
public interface AdminItemApiSpecV1 {

    @Operation(summary = "아이템 등록", description = "아이템을 신규 등록합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Id>> create(@Valid @RequestBody AdminItemRequest.Create request);

    @Operation(summary = "아이템 수정", description = "아이템 속성을 일부 수정합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Id>> update(
            @PathVariable Long itemId,
            @Valid @RequestBody AdminItemRequest.Update request
    );

    @Operation(summary = "아이템 삭제", description = "아이템을 삭제합니다.")
    ResponseEntity<ApiResponse<AdminItemResponse.Deleted>> delete(@PathVariable Long itemId);
}
