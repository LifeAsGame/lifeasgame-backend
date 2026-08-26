package online.lifeasgame.inventory.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.AdminInventoryEntitlementService;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.api.admin.mapper.AdminMailboxWebMapper;
import online.lifeasgame.inventory.api.admin.request.AdminMailboxRequest;
import online.lifeasgame.inventory.api.admin.response.AdminMailboxResponse;
import online.lifeasgame.inventory.api.admin.spec.AdminMailboxApiSpecV1;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminMailboxController implements AdminMailboxApiSpecV1 {

    private final AdminInventoryEntitlementService entitlementService;

    @Override
    @PostMapping("/{playerId}/mailbox/deliver")
    public ResponseEntity<ApiResponse<AdminMailboxResponse.Slot>> deliverToMailbox(
            @PathVariable @Positive Long playerId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")
            String correlationId,
            @Valid @RequestBody AdminMailboxRequest.Deliver request
    ) {
        MailboxResult.Slot result = entitlementService.deliverToMailbox(
                AdminMailboxWebMapper.toDeliverCommand(
                        playerId,
                        request,
                        idempotencyKey,
                        correlationId
                )
        );
        return ApiResponses.ok(AdminMailboxWebMapper.toSlot(result));
    }
}
