package online.lifeasgame.inventory.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.inventory.application.MailboxQueryService;
import online.lifeasgame.inventory.application.MailboxService;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.api.player.mapper.MailboxWebMapper;
import online.lifeasgame.inventory.api.player.request.MailboxRequest;
import online.lifeasgame.inventory.api.player.response.MailboxResponse;
import online.lifeasgame.inventory.api.player.spec.MailboxApiSpecV1;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mailbox")
public class MailboxController implements MailboxApiSpecV1 {

    private final MailboxService mailboxService;
    private final MailboxQueryService mailboxQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<MailboxResponse.Entries>> list() {
        MailboxResult.Entries result = mailboxQueryService.list();
        return ApiResponses.ok(MailboxWebMapper.toMails(result));
    }

    @Override
    @PostMapping("/claim")
    public ResponseEntity<ApiResponse<Void>> claim(@Valid @RequestBody MailboxRequest.Claim request) {
        mailboxService.claim(MailboxWebMapper.toClaimCommand(request));
        return ApiResponses.noContent();
    }

    @Override
    @PostMapping("/claim/all")
    public ResponseEntity<ApiResponse<Void>> claimAll(
            @Valid @RequestBody MailboxRequest.ClaimAll request
    ) {
        mailboxService.claimAll(MailboxWebMapper.toClaimAllCommand(request));
        return ApiResponses.noContent();
    }

    @Override
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(MailboxRequest.Delete request) {
        mailboxService.delete(MailboxWebMapper.toDeleteCommand(request));
        return ApiResponses.noContent();
    }
}
