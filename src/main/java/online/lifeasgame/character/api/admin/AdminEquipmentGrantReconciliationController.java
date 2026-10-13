package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.EquipmentGrantReconciliation;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/equipment-grants")
public class AdminEquipmentGrantReconciliationController {
    private final EquipmentGrantReconciliation reconciliation;

    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<EquipmentGrantReconciliation.Batch>> reconcile(
            @Valid @RequestBody Request request) {
        return ApiResponses.ok(reconciliation.run(
                request.afterPlayerId(), request.batchSize(), request.apply()));
    }

    public record Request(@Min(0) long afterPlayerId,
                          @Min(1) @Max(100) int batchSize,
                          @NotNull Boolean apply) {}
}
