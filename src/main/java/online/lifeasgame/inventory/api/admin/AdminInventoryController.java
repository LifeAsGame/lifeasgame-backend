package online.lifeasgame.inventory.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.AdminInventoryEntitlementService;
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

    private final AdminInventoryEntitlementService entitlementService;

    @Override
    @PostMapping("/{playerId}/inventory/add")
    public ResponseEntity<ApiResponse<AdminInventoryResponse.Slots>> addToInventory(
            @PathVariable @Positive Long playerId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")
            String correlationId,
            @Valid @RequestBody AdminInventoryRequest.Add request
    ) {
        InventoryResult.Slots result = entitlementService.addToInventory(
                AdminInventoryWebMapper.toAddCommand(
                        playerId,
                        request,
                        idempotencyKey,
                        correlationId
                )
        );
        return ApiResponses.ok(AdminInventoryWebMapper.toSlots(result));
    }
}
