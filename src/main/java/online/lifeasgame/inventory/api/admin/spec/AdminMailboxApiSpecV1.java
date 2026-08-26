package online.lifeasgame.inventory.api.admin.spec;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.api.admin.request.AdminMailboxRequest;
import online.lifeasgame.inventory.api.admin.response.AdminMailboxResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Mailbox API V1 (Admin)")
public interface AdminMailboxApiSpecV1 {

    @Operation(summary = "우편 지급(관리자)", description = "특정 플레이어 우편함에 아이템을 지급합니다.")
    ResponseEntity<ApiResponse<AdminMailboxResponse.Slot>> deliverToMailbox(
            @PathVariable @Positive Long playerId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")
            String correlationId,
            @Valid @RequestBody AdminMailboxRequest.Deliver request
    );
}
