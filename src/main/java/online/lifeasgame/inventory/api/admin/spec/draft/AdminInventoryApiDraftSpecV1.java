package online.lifeasgame.inventory.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.admin.request.AdminInventoryRequest;
import online.lifeasgame.inventory.api.admin.response.AdminInventoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface AdminInventoryApiDraftSpecV1 {

    @Operation(summary = "플레이어 인벤토리 조회(관리자)", description = "플레이어 인벤토리 메타 + 엔트리를 조회합니다.")
    ResponseEntity<ApiResponse<AdminInventoryResponse.View>> view(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "true") boolean includeItem,
            @RequestParam(defaultValue = "true") boolean includeInstanceAttrs
    );

    @Operation(summary = "플레이어 인벤토리 엔트리 단건 조회(관리자)", description = "itemInstanceId 기준 엔트리 단건을 조회합니다.")
    ResponseEntity<ApiResponse<AdminInventoryResponse.EntryDetail>> getEntry(
            @PathVariable Long playerId,
            @PathVariable Long itemInstanceId,
            @RequestParam(defaultValue = "true") boolean includeItem,
            @RequestParam(defaultValue = "true") boolean includeInstanceAttrs
    );

    @Operation(summary = "플레이어 인벤토리 용량 변경(관리자)", description = "capacitySlots를 변경합니다.")
    ResponseEntity<ApiResponse<AdminInventoryResponse.Meta>> setCapacity(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminInventoryRequest.SetCapacity request
    );
}
