package online.lifeasgame.character.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "장비 지급 조정", description = "실패 시 이미 지급된 건수를 반환하고, nextAfterPlayerId는 마지막 성공 Player를 가리킵니다. "
            + "hasMore=true와 failedPlayerIds를 확인해 실패 원인을 해결한 뒤 같은 cursor로 재시도하세요. "
            + "반복 실패하는 Player를 건너뛰는 결정은 운영자가 별도로 내려야 합니다.")
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
