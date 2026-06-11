package online.lifeasgame.inventory.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.InventoryService;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.api.admin.mapper.AdminInventoryWebMapper;
import online.lifeasgame.inventory.api.admin.request.AdminInventoryRequest;
import online.lifeasgame.inventory.api.admin.response.AdminInventoryResponse;
import online.lifeasgame.inventory.api.admin.spec.AdminInventoryApiSpecV1;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminInventoryController implements AdminInventoryApiSpecV1 {

    private final InventoryService inventoryService;

    @Override
    @PostMapping("/{playerId}/inventory/add")
    public ResponseEntity<ApiResponse<AdminInventoryResponse.Slots>> addToInventory(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminInventoryRequest.Add request
    ) {
        InventoryResult.Slots result = inventoryService.add(playerId, AdminInventoryWebMapper.toAddCommand(request));
        return ApiResponses.ok(AdminInventoryWebMapper.toSlots(result));
    }
}
