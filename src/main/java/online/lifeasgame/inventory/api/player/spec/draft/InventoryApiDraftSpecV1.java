package online.lifeasgame.inventory.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.player.response.InventoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface InventoryApiDraftSpecV1 {
    @Operation(summary = "인벤토리 화면 조회", description = "capacity/used/free 메타 + 엔트리(옵션: 아이템/스탯/내구도/인스턴스속성 포함)를 조회합니다.")
    ResponseEntity<ApiResponse<InventoryResponse.View>> view(
            @RequestParam(defaultValue = "true") boolean includeItem,
            @RequestParam(defaultValue = "true") boolean includeInstanceAttrs
    );

    @Operation(summary = "인벤토리 엔트리 단건 조회", description = "itemInstanceId(=InventoryEntry.id) 기준으로 엔트리 상세를 조회합니다.")
    ResponseEntity<ApiResponse<InventoryResponse.EntryDetail>> getEntry(
            @PathVariable Long itemInstanceId,
            @RequestParam(defaultValue = "true") boolean includeItem,
            @RequestParam(defaultValue = "true") boolean includeInstanceAttrs
    );


}
