package online.lifeasgame.inventory.api.admin;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.AdminInventoryEntitlementService;
import online.lifeasgame.inventory.application.MailboxQueryService;
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
    private final MailboxQueryService mailboxQueryService;

    @Override
    @GetMapping("/{playerId}/mailbox")
    public ResponseEntity<ApiResponse<AdminMailboxResponse.Entries>> listMailbox(
            @PathVariable Long playerId
    ) {
        MailboxResult.Entries result = mailboxQueryService.list(playerId);
        return ApiResponses.ok(AdminMailboxWebMapper.toEntries(playerId, result));
    }

    @Override
    @PostMapping("/{playerId}/mailbox/deliver")
    public ResponseEntity<ApiResponse<AdminMailboxResponse.Slot>> deliverToMailbox(
            @PathVariable Long playerId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody AdminMailboxRequest.Deliver request
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
