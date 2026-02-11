package online.lifeasgame.inventory.api.admin.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.admin.request.AdminMailboxRequest;
import online.lifeasgame.inventory.api.admin.response.AdminMailboxResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface AdminMailboxApiDraftSpecV1 {

    @Operation(summary = "플레이어 우편함 조회(관리자)", description = "플레이어 우편함 메타 + 메일을 조회합니다.")
    ResponseEntity<ApiResponse<AdminMailboxResponse.View>> view(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "true") boolean includeItem,
            @RequestParam(defaultValue = "true") boolean includeInstanceAttrs
    );

    @Operation(summary = "우편 삭제(관리자)", description = "특정 슬롯의 우편을 삭제합니다(보상 폐기).")
    ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminMailboxRequest.Delete request
    );
}
