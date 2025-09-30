package online.lifeasgame.inventory.api.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.MailboxService;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.api.admin.mapper.AdminMailboxWebMapper;
import online.lifeasgame.inventory.api.admin.request.AdminMailboxRequest;
import online.lifeasgame.inventory.api.admin.reseponse.AdminMailboxResponse;
import online.lifeasgame.inventory.api.admin.spec.AdminMailboxApiSpecV1;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/players")
public class AdminMailboxController implements AdminMailboxApiSpecV1 {

    private final MailboxService mailboxService;

    @Override
    @PostMapping("/{playerId}/mailbox/deliver")
    public ResponseEntity<ApiResponse<AdminMailboxResponse.Slot>> deliverToMailbox(
            @PathVariable Long playerId,
            @Valid @RequestBody AdminMailboxRequest.Deliver request
    ) {
        MailboxResult.Slot adminResult =
                mailboxService.deliver(playerId, AdminMailboxWebMapper.toCommand(request));
        return ApiResponses.ok(AdminMailboxWebMapper.toSlot(adminResult));
    }
}
