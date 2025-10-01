package online.lifeasgame.inventory.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.admin.request.AdminInventoryRequest;
import online.lifeasgame.inventory.api.admin.response.AdminInventoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Inventory API V1 (Admin)")
public interface AdminInventoryApiSpecV1 {

    @Operation(summary = "인벤토리 아이템 지급(관리자)", description = "특정 플레이어 인벤토리에 아이템을 지급합니다.")
    ResponseEntity<ApiResponse<AdminInventoryResponse.Slots>> addToInventory(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminInventoryRequest.Add request
    );
}
